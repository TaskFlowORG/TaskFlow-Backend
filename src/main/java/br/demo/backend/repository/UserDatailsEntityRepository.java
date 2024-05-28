package br.demo.backend.repository;

import br.demo.backend.security.entity.UserDatailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDatailsEntityRepository extends JpaRepository<UserDatailEntity, Long> {
}
