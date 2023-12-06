package br.demo.backend.service;


import br.demo.backend.model.Group;
import br.demo.backend.model.Permission;
import br.demo.backend.model.Project;
import br.demo.backend.model.User;
import br.demo.backend.repository.GroupRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@AllArgsConstructor
public class GroupService {

    private GroupRepository groupRepository;
    private ProjectService projectService;

    public Collection<Group> findAll() {
        Collection<Group> groups = groupRepository.findAll();
        for (Group group : groups) {
            ResolveStackOverflow.resolveStackOverflow(group);
        }
        return groups;
    }

    public Group findOne(Long id) {
        Group group = groupRepository.findById(id).get();
        ResolveStackOverflow.resolveStackOverflow(group);
        return group;
    }

    public void save(Group group) {
        groupRepository.save(group);
    }

    public Collection<Group> findGroupsByUser(Long userId) {
        Collection<Group> groups = groupRepository.findGroupsByUsersContaining(new User(userId));
        for (Group group : groups) {
            ResolveStackOverflow.resolveStackOverflow(group);
        }
        return groups;
    }

    public Permission getPermissionOfAGroupInAProject(Long groupId, Long projectId) {
        Group group = groupRepository.findById(groupId).get();
        Permission permission = group.getPermission().stream().filter(
                p -> p.getProject().getId().equals(projectId)
        ).findFirst().get();
        return permission;
    }


    public Collection<Group> findGroupsOfAProject(Long projectId) {
        Collection<Group> groups = groupRepository.findGroupsByPermission_Project(new Project(projectId));
        for (Group group : groups) {
            ResolveStackOverflow.resolveStackOverflow(group);
        }
        return groups;
    }

    public void update(Group group) {
        Group groupOld = groupRepository.findById(group.getId()).get();
        for (Permission permissionOld : groupOld.getPermission()) {
            for (Permission permission : group.getPermission()) {
                if (permissionOld.getProject().getId().equals(permission.getProject().getId()) &&
                        !permissionOld.getPermission().equals(permission.getPermission())) {
                    updatePermission(group, permission.getProject().getId(), permission);
                    break;
                }
            }
        }
        groupRepository.save(group);
    }


    private void updatePermission(Group group, Long projectId, Permission permission) {
        for (User user : group.getUsers()) {
            for (Permission permissionFor : user.getPermission()) {
                if (permissionFor.getProject().getId().equals(projectId)) {
                    user.getPermission().remove(permissionFor);
                    break;
                }
            }
            user.getPermission().add(permission);
        }
        groupRepository.save(group);
    }


    public void delete(Long id) {
        groupRepository.deleteById(id);
    }
}
