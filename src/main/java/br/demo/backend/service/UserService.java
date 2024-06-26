package br.demo.backend.service;


import br.demo.backend.exception.CurrentPasswordDontMatchException;
import br.demo.backend.exception.HeHaveGroupsException;
import br.demo.backend.exception.HeHaveProjectsException;
import br.demo.backend.exception.UsernameAlreadyUsedException;
import br.demo.backend.model.*;
import br.demo.backend.model.dtos.user.*;
import br.demo.backend.model.enums.TypeOfNotification;
import br.demo.backend.repository.GroupRepository;
import br.demo.backend.repository.PermissionRepository;
import br.demo.backend.repository.ProjectRepository;
import br.demo.backend.security.entity.UserDatailEntity;
import br.demo.backend.security.exception.ForbiddenException;
import br.demo.backend.security.service.AuthenticationService;
import br.demo.backend.utils.AutoMapper;
import br.demo.backend.utils.ModelToGetDTO;
import br.demo.backend.model.dtos.permission.PermissionGetDTO;
import br.demo.backend.repository.UserRepository;
import br.demo.backend.utils.ResizeImage;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private ProjectRepository projectRepository;
    private AutoMapper<User> autoMapper;
    private NotificationService notificationService;
    private PermissionRepository permissionRepository;
    private AuthenticationManager authenticationManager;


    //find one, normally used to find a user in the same group or project
    public OtherUsersDTO findOne(String id) {
        User user = userRepository.findByUserDetailsEntity_Username(id).get();
        return ModelToGetDTO.transformOther(user);
    }

    //save, normally used to create a new user with a unique username
    public UserGetDTO save(UserPostDTO userDto) {
        try {
            userRepository.findByUserDetailsEntity_Username(userDto.getUserDetailsEntity().getUsername()).get();
            throw new UsernameAlreadyUsedException();
        } catch (NoSuchElementException e) {
            User user = new User();
            BeanUtils.copyProperties(userDto, user);
            user.setConfiguration(new Configuration());
            user.getUserDetailsEntity().setLastPasswordEdition(OffsetDateTime.now());
            return ModelToGetDTO.tranform(userRepository.save(user));
        }
    }

    public UserGetDTO update(UserPutDTO userDTO, Boolean patching) throws AccessDeniedException {
        String username = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User oldUser = userRepository.findById(userDTO.getId()).get();
        //if the user is trying to change another user
        if (!oldUser.getUserDetailsEntity().getUsername().equals(username)) {
            throw new ForbiddenException();
        }

        User user = new User();
        if (patching) BeanUtils.copyProperties(oldUser, user);
        autoMapper.map(userDTO, user, patching);

        keepFields(user, oldUser);

        return ModelToGetDTO.tranform(userRepository.save(user));
    }

    public void changeUsername(UserChangeUsernameDTO userChangeUsernameDTO){
        UserDatailEntity userDetails = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User user = userRepository.findByUserDetailsEntity_Username(userDetails.getUsername()).get();

        if(userRepository.findByUserDetailsEntity_Username(userChangeUsernameDTO.getUsername()).isPresent()){
            throw new UsernameAlreadyUsedException();
        }
        if(userRepository.findByUserDetailsEntity_Username(userChangeUsernameDTO.getUsername()).equals(userChangeUsernameDTO.getUsername())){
            throw new UsernameAlreadyUsedException();
        }
        user.getUserDetailsEntity().setUsername(userChangeUsernameDTO.getUsername());
        ModelToGetDTO.tranform(userRepository.save(user));
    }

    public void changePassword(UserChangePasswordDTO userChangePasswordDTO){
        UserDatailEntity userDetails = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User user = userRepository.findById(userDetails.getId()).get();

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(user.getUserDetailsEntity().getUsername(), userChangePasswordDTO.getPassword());
        try {
            authenticationManager.authenticate(token);
            user.getUserDetailsEntity().setPassword(userChangePasswordDTO.getNewPassword());
            user.getUserDetailsEntity().setCredentialsNonExpired(true);
            user.getUserDetailsEntity().setLastPasswordEdition(OffsetDateTime.now());
            ModelToGetDTO.tranform(userRepository.save(user));
        }catch (AuthenticationException e){
            throw new CurrentPasswordDontMatchException();
        }

    }

    private void keepFields(User user, User oldUser){
        //keep some fields that can't be changed
        user.setPicture(oldUser.getPicture());
        user.setPoints(oldUser.getPoints());
        user.setUserDetailsEntity(oldUser.getUserDetailsEntity());
    }

    public void delete() {
        String username = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        validateDelete(username);
        User user = userRepository.findByUserDetailsEntity_Username(username).get();
        // thats not a real delete, just a disable for a time
        user.getUserDetailsEntity().setEnabled(false);
        user.getUserDetailsEntity().setWhenHeTryDelete(OffsetDateTime.now());
        userRepository.save(user);
    }

    private void validateDelete(String username) {
        Collection<Project> projects = projectRepository.findProjectsByOwner_UserDetailsEntity_Username(username);

        if (!projects.isEmpty()) {
            projects.forEach(p -> {
                Collection<Group> groups = groupRepository.findGroupsByPermissions_Project(p);
                if (!groups.isEmpty()) {
                    throw new HeHaveProjectsException();
                }
            });
        }
        Collection<Group> groups = groupRepository.findGroupsByOwner_UserDetailsEntity_Username(username);
        if (!groups.isEmpty() && groups.stream().anyMatch(g -> !g.getUsers().isEmpty())) {
            throw new HeHaveGroupsException();
        }
    }

    public UserGetDTO updatePicture(MultipartFile picture) {
        String username = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = userRepository.findByUserDetailsEntity_Username(username).get();
        user.setPicture(new Archive(picture));
        try {
            user.getPicture().setData(ResizeImage.resizeImage(picture, 200, 200));
        } catch (IOException ignore) {}
        return ModelToGetDTO.tranform(userRepository.save(user));
    }

    public UserGetDTO updatePassword(String id, String password) {
        User user = userRepository.findByUserDetailsEntity_Username(id).get();
        user.getUserDetailsEntity().setPassword(password);
        user.getUserDetailsEntity().setCredentialsNonExpired(true);
        user.getUserDetailsEntity().setLastPasswordEdition(OffsetDateTime.now());
        return ModelToGetDTO.tranform(userRepository.save(user));
    }


    public Collection<OtherUsersDTO> findAll() {
        String username = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = userRepository.findByUserDetailsEntity_Username(username).get();

        return userRepository.findAll().stream().map(ModelToGetDTO::transformOther).toList();
    }

    public void addPoints(User user, Long points) {
        List<Long> targets = List.of(1000L, 5000L, 10000L, 15000L, 30000L, 50000L, 100000L, 200000L, 500000L, 1000000L);
        //check if the user reached a target
        for (Long target : targets) {
            if (user.getPoints() < target && user.getPoints() + points >= target) {
                notificationService.generateNotification(TypeOfNotification.POINTS, user.getId(), target);
            }
        }
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }

    public UserGetDTO findLogged() {
        String username = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return ModelToGetDTO.tranform(userRepository.findByUserDetailsEntity_Username(username).get());
    }

    public PermissionGetDTO updatePermissionOfAUser(String username, Permission permissionTemp) {
        Permission permission = permissionRepository.findById(permissionTemp.getId()).get();
        User user = userRepository.findByUserDetailsEntity_Username(username).get();
        Collection<Permission> permissions = getNewPermissions(user, permission);
        permissions.add(permission);
        user.setPermissions(permissions);
        userRepository.save(user);
        return ModelToGetDTO.tranform(permission);
    }

    private Collection<Permission> getNewPermissions(User user, Permission permission) {
        Collection<Permission> permissions = user.getPermissions();
        try {
            //if the user already have a permission in the project
            Permission oldPermission = getOldPermission(user, permission);
            if (oldPermission == null || !oldPermission.equals(permission)) {
                notificationService.generateNotification(TypeOfNotification
                        .CHANGEPERMISSION, user.getId(), permission.getProject().getId());
            }
            //remove the old permission
            if(oldPermission != null) permissions.remove(oldPermission);
        } catch (NullPointerException e) {
            return new ArrayList<>();
        }
        return permissions;
    }

    private Permission getOldPermission(User user, Permission permission) {
        return user.getPermissions()
                .stream().filter(p -> p.getProject().equals(permission.getProject()))
                .findFirst().orElse(null);
    }

    public void getOutOfGroup(Long groupId) {
        String username = ((UserDatailEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = userRepository.findByUserDetailsEntity_Username(username).get();
        Group group = groupRepository.findById(groupId).get();
        Collection<Project> projects = group.getPermissions().stream().map(Permission::getProject).toList();
        user.getPermissions().removeAll(user.getPermissions().stream().filter(p-> projects.contains(p.getProject())).toList());
        group.getUsers().remove(user);
        userRepository.save(user);
        groupRepository.save(group);
    }


    public void updateAllPermissions(String username, List<Permission> permissions) {
        User user = userRepository.findByUserDetailsEntity_Username(username).get();
        user.setPermissions(permissions);
        userRepository.save(user);
    }
}


