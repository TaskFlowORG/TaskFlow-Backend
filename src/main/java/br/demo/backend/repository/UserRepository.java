package br.demo.backend.repository;

import br.demo.backend.model.Permission;
import br.demo.backend.model.Project;
import br.demo.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User>findByUserDetailsEntity_Username(String username);
    Optional<User>findByUserDetailsEntity_UsernameContainingOrNameContaining(String username, String name);
    Collection<User> findAllByPermissions_Project(Project project);

    Collection<User> findByPermissionsContaining(Permission permission);

    Optional<User> findByUserDetailsEntity_UsernameGitHub(String username);

    Optional<User> findByMail(String email);

}
