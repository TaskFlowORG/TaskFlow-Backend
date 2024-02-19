package br.demo.backend.repository.values;

import br.demo.backend.model.values.TextValued;
import br.demo.backend.model.values.TimeValued;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeValuedRepository extends JpaRepository<TimeValued, Long> {
}
