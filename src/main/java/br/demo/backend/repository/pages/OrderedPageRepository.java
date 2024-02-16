package br.demo.backend.repository.pages;
import br.demo.backend.model.pages.OrderedPage;
import br.demo.backend.model.relations.TaskOrdered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderedPageRepository extends JpaRepository<OrderedPage, Long> {


}