package br.demo.backend.service;


import br.demo.backend.model.Page;
import br.demo.backend.repository.PageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@AllArgsConstructor
public class PageService {

    PageRepository pageRepository;

    public Collection<Page> findAll() {
        return pageRepository.findAll();
    }

    public Page findOne(Long id) {
        return pageRepository.findById(id).get();
    }

    public void save(Page page) {
        pageRepository.save(page);
    }

    public void delete(Long id) {
        pageRepository.deleteById(id);
    }
}
