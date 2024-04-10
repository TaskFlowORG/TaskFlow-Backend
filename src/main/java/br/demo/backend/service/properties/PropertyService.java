package br.demo.backend.service.properties;


import br.demo.backend.exception.PropertyCantBeDeletedException;
import br.demo.backend.model.Project;
import br.demo.backend.model.enums.TypeOfPage;
import br.demo.backend.model.enums.TypeOfProperty;
import br.demo.backend.model.interfaces.IHasProperties;
import br.demo.backend.model.pages.Page;
import br.demo.backend.model.properties.Date;
import br.demo.backend.model.properties.Limited;
import br.demo.backend.model.properties.Property;
import br.demo.backend.model.properties.Select;
import br.demo.backend.model.relations.PropertyValue;
import br.demo.backend.repository.ProjectRepository;
import br.demo.backend.repository.pages.OrderedPageRepository;
import br.demo.backend.repository.pages.PageRepository;
import br.demo.backend.repository.properties.DateRepository;
import br.demo.backend.repository.properties.LimitedRepository;
import br.demo.backend.repository.properties.PropertyRepository;
import br.demo.backend.repository.properties.SelectRepository;
import br.demo.backend.service.PropertyValueService;
import br.demo.backend.utils.AutoMapper;
import br.demo.backend.repository.relations.PropertyValueRepository;
import br.demo.backend.repository.tasks.TaskRepository;
import br.demo.backend.service.tasks.TaskService;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class PropertyService {

    private PropertyRepository propertyRepository;
    private ProjectRepository projectRepository;
    private PageRepository pageRepository;
    private TaskService taskService;
    private LimitedRepository limitedRepository;
    private TaskRepository taskRepository;
    private SelectRepository selectRepository;
    private DateRepository dateRepository;
    private AutoMapper<Limited> autoMapperLimited;
    private AutoMapper<Select> autoMapperSelect;
    private AutoMapper<Date> autoMapperDate;
    private OrderedPageRepository orderedPageRepository;
    private PropertyValueRepository taskValueRepository;
    private PropertyValueService propertyValueService;

    //that method is used to add the new property to the tasks that already exists
    private Property setInTheTasksThatAlreadyExists(Property property) {
        if (property.getPages() != null) {
            return setRelationAtPage(property, property.getPages());
        } else {
            Project project = projectRepository.findById(property.getProject().getId()).get();
            return setRelationAtPage(property, project.getPages());
        }
    }

    private Property setRelationAtPage(Property property, Collection<Page> pages) {
        pages.stream().forEach(p -> {
            //get the page from the database
            Page page = pageRepository.findById(p.getId()).get();
            //get the tasks from the page
            page.getTasks().stream().map(tP -> {
                //add the property to the task
                tP.getTask().getProperties().add(propertyValueService.setTaskProperty(property));
                taskService.update(tP.getTask(), true);
                return tP;
            }).toList();
        });
        return property;
    }

    private <T extends Property> T saveGeneric(T property, JpaRepository<T, Long> repo) {
        T prop = repo.save(property);
        setInTheTasksThatAlreadyExists(prop);
        return prop;
    }

    public Limited save(Limited property) {
        return saveGeneric(property, limitedRepository);
    }

    public Date save(Date property) {
        return saveGeneric(property, dateRepository);
    }

    public Select save(Select property) {
        return saveGeneric(property, selectRepository);
    }

    private <T extends Property> void  updateGeneric(T property, Boolean patching,
                                                     AutoMapper<T> autoMapper,
                                                     JpaRepository<T, Long> repo,
                                                     T empty){
        T old = repo.findById(property.getId()).get();
        T prop = patching ? old : empty;
        autoMapper.map(property, prop, patching, true);
        //this keep the type, pages and project of the property
        prop.setType(old.getType());
        prop.setPages(old.getPages());
        prop.setProject(old.getProject());
        repo.save(property);
    }

    public void update(Limited propertyDTO, Boolean patching) {
        updateGeneric(propertyDTO, patching, autoMapperLimited, limitedRepository, new Limited());
    }

    public void update(Date propertyDTO, Boolean patching) {
        updateGeneric(propertyDTO, patching, autoMapperDate, dateRepository, new Date());
    }

    public void update(Select propertyDTO, Boolean patching) {
        updateGeneric(propertyDTO, patching, autoMapperSelect, selectRepository, new Select());
    }

    public void delete(Long id) {
        Property property = propertyRepository.findById(id).get();
        if (validateCanBeDeleted(property)) {
            orderedPageRepository.findAllByPropertyOrdering_Id(property.getId())
                    .forEach(p -> {
                List<TypeOfProperty> types = getPossibleSubstitutesTypes(p.getType());
                Property newPropOrd = getOtherProp(p, property, types);
                if(newPropOrd == null) newPropOrd = getOtherProp(p.getProject(), property, types);
                p.setPropertyOrdering(newPropOrd);
                orderedPageRepository.save(p);
            });
            //this disassociate the property from the tasks
            disassociatePropertyFromTasks(property);
            propertyRepository.delete(property);
        } else {
            throw  new PropertyCantBeDeletedException();
        }
    }

    private void disassociatePropertyFromTasks(Property property) {
        taskRepository.findAll().stream().forEach(t -> {
            //here we filter to find the value to this prop
            PropertyValue propertyValue = t.getProperties().stream().filter(p ->
                    p.getProperty().equals(property)).findFirst().orElse(null);
            if(propertyValue != null){
                //and here we delete the propvalue and save the task without it
                t.getProperties().remove(propertyValue);
                taskService.update(t, true);
                taskValueRepository.deleteById(propertyValue.getId());
            }
        });
    }

    private Boolean validateCanBeDeleted(Property property) {
        TypeOfPage typeOfPage = getTypeOfDependetPage(property);
        if(typeOfPage == null){
            return true;
        }
        List <TypeOfProperty> typesOfProperty = getPossibleSubstitutesTypes(typeOfPage);
        if(property.getProject()!=null){
            return testInProject(typeOfPage, property, typesOfProperty);
        }else{
            return testInPages(typeOfPage, property, property.getPages(), typesOfProperty);
        }
    }

    private List<TypeOfProperty> getPossibleSubstitutesTypes(TypeOfPage type) {
        return switch (type) {
            case KANBAN -> List.of(TypeOfProperty.SELECT, TypeOfProperty.RADIO, TypeOfProperty.CHECKBOX, TypeOfProperty.TAG);
            case CALENDAR -> List.of(TypeOfProperty.DATE);
            case TIMELINE -> List.of(TypeOfProperty.TIME);
            default -> null;
        };
    }

    private TypeOfPage getTypeOfDependetPage(Property property) {
        return switch (property.getType()) {
            case SELECT, RADIO, CHECKBOX, TAG -> TypeOfPage.KANBAN;
            case DATE -> TypeOfPage.CALENDAR;
            case TIME -> TypeOfPage.TIMELINE;
            default -> null;
        };
    }



    //thats the method test if exists some property of some types at the project
    private Boolean testInProject(TypeOfPage typeOfPage, Property property,
                                  List<TypeOfProperty> typesOfProperty) {
        Project project = projectRepository.findById(property.getProject().getId()).get();
        if (project.getProperties().stream().anyMatch(p ->
                !p.equals(property) &&
                        typesOfProperty.contains(p.getType()))) {
            return true;
        } else return testInPages( typeOfPage, property, project.getPages(), typesOfProperty);
    }

    //thats the method test if exists some property of some types at the pages
    private Boolean testInPages(TypeOfPage typeOfPage, Property property,
                                Collection<Page> pages,List<TypeOfProperty> typesOfProperty) {
        return pages.stream().allMatch(p -> {
            if (typeOfPage.equals(p.getType())) {
                return getOtherProp(p, property, typesOfProperty) != null;
            }
            return true;
        });
    }

    //this find sme other property in a page or project
    private Property getOtherProp(IHasProperties p, Property property,
                                  List<TypeOfProperty> typesOfProperty) {
        return p.getProperties().stream().filter(prop ->
                        !prop.equals(property)
                                && typesOfProperty.contains(prop.getType()))
                .findFirst().orElse(null);
    }
}
