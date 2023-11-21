package br.demo.backend.repository;

import br.demo.backend.model.TaskPageId;
import br.demo.backend.model.TaskPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskPageRepository extends JpaRepository<TaskPage, TaskPageId>{
}
