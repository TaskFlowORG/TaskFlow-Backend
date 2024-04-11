package br.demo.backend.service;


import br.demo.backend.model.Group;
import br.demo.backend.model.Project;
import br.demo.backend.model.User;
import br.demo.backend.model.dtos.group.GroupPutDTO;
import br.demo.backend.repository.GroupRepository;
import br.demo.backend.repository.ProjectRepository;
import br.demo.backend.repository.UserRepository;
import br.demo.backend.utils.AutoMapper;
import br.demo.backend.utils.ModelToGetDTO;
import br.demo.backend.model.Permission;
import br.demo.backend.model.dtos.permission.PermissionGetDTO;
import br.demo.backend.model.dtos.permission.PermissionPostDTO;
import br.demo.backend.model.dtos.permission.PermissionPutDTO;
import br.demo.backend.model.enums.TypePermission;
import br.demo.backend.repository.PermissionRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@AllArgsConstructor
public class PermissionService {

    private PermissionRepository permissionRepository;
    private AutoMapper<Permission> autoMapper;
    private ProjectRepository projectRepository;
    private UserRepository useRepository;
    private GroupRepository groupRepository;
    private GroupService groupService;
    private UserService userService;

    public PermissionGetDTO save(PermissionPostDTO permissionDto, Long projectId) {
        Project project = projectRepository.findById(projectId).get();
        Permission permission = new Permission();
        BeanUtils.copyProperties(permissionDto, permission);
        permission.setProject(project);
        return ModelToGetDTO.tranform(permissionRepository.save(permission));
    }

    public PermissionGetDTO update(PermissionPutDTO permissionDto, Boolean patching) {
        Permission oldPermission = permissionRepository.findById(permissionDto.getId()).get();
        Permission permission = patching ? oldPermission : new Permission();
        autoMapper.map(permissionDto, permission, patching);
        //keep the project of the permission
        permission.setProject(oldPermission.getProject());
        return ModelToGetDTO.tranform(permissionRepository.save(permission));
    }

    //find the permissions of a project
    public Collection<PermissionGetDTO> findByProject(Long projectId) {
        Project project = projectRepository.findById(projectId).get();
        return permissionRepository.findByProject(project).stream().map(ModelToGetDTO::tranform).toList();
    }

    public void delete(Long id, Long substituteId) {
        Permission otherPermission = permissionRepository.findById(substituteId).get();
        Permission permission = permissionRepository.findById(id).get();

        changeInTheGroup(permission, otherPermission);
        changeInTheUser(permission, otherPermission);

        permissionRepository.deleteById(id);
    }

    private void changeInTheUser(Permission permission, Permission otherPermission){
        Collection<User> users = useRepository.findByPermissionsContaining(permission);
        users.forEach(
                u -> {
                    u.getPermissions().remove(permission);
                    u.getPermissions().add(otherPermission);
                    userService.updatePermissionOfAUser(u.getUserDetailsEntity().getUsername(), otherPermission);
                }
        );
    }

    private void changeInTheGroup(Permission permission, Permission otherPermission){
        Collection<Group> groups = groupRepository.findByPermissionsContaining(permission);
        groups.forEach(
                g -> {
                    g.getPermissions().remove(permission);
                    g.getPermissions().add(otherPermission);
                    GroupPutDTO groupPutDTO = new GroupPutDTO();
                    BeanUtils.copyProperties(g, groupPutDTO);
                    groupService.update(groupPutDTO, false);
                }
        );
    }
}
