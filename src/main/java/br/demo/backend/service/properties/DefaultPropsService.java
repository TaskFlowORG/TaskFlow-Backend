package br.demo.backend.service.properties;

import br.demo.backend.model.Project;
import br.demo.backend.model.enums.TypeOfProperty;
import br.demo.backend.model.pages.OrderedPage;
import br.demo.backend.model.pages.Page;
import br.demo.backend.model.properties.Date;
import br.demo.backend.model.properties.Limited;
import br.demo.backend.model.properties.Option;
import br.demo.backend.model.properties.Select;
import br.demo.backend.repository.properties.DateRepository;
import br.demo.backend.repository.properties.LimitedRepository;
import br.demo.backend.repository.properties.SelectRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@Service
public class DefaultPropsService {

    private DateRepository dateRepository;
    private SelectRepository selectRepository;
    private LimitedRepository limitedRepository;

    public Select select(Project project, Page page){
        ArrayList<Page> pages = new ArrayList<>();
        Select select = new Select(null, "Stats", true, false,
                createOptions(), TypeOfProperty.SELECT, pages, project);
        if(page == null){
            project.setProperties(new ArrayList<>());
            select = selectRepository.save(select);
            project.getProperties().add(select);
        }else{
            pages.add(page);
            page.setProperties(new ArrayList<>());
            select = selectRepository.save(select);
            page.getProperties().add(select);
        }
        return select;
    }

    private Collection<Option> createOptions(){
        return List.of(
            new Option(null, "To-do", "#FF7A00", 0),
            new Option(null, "Doing", "#F7624B", 1),
            new Option(null, "Done", "#F04A94", 2)
        );
    }

    public Date date(Project project, Page page){
        ArrayList<Page> pages = new ArrayList<>();
        pages.add(page);
        Date date = new Date(null, "Date", true, false, pages, project);
        page.setProperties(new ArrayList<>());
        Date dateSaved = dateRepository.save(date);
        page.getProperties().add(dateSaved);
        return dateSaved;
    }

    public Limited limited(Project project, OrderedPage page) {
        ArrayList<Page> pages = new ArrayList<>();
        pages.add(page);
        Limited limited = new Limited(null, "Time", true, false,
                TypeOfProperty.TIME, pages, project, 28800L );
        page.setProperties(new ArrayList<>());
        Limited limitedSaved = limitedRepository.save(limited);
        page.getProperties().add(limitedSaved);
        return limitedSaved;
    }
}
