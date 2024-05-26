package br.demo.backend.service.properties;


import br.demo.backend.exception.PropertyCantBeDeletedException;
import br.demo.backend.model.Project;
import br.demo.backend.model.dtos.properties.PropertyGetDTO;
import br.demo.backend.model.enums.TypeOfPage;
import br.demo.backend.model.enums.TypeOfProperty;
import br.demo.backend.interfaces.IHasProperties;
import br.demo.backend.model.pages.Page;
import br.demo.backend.model.properties.Date;
import br.demo.backend.model.properties.Limited;
import br.demo.backend.model.properties.Property;
import br.demo.backend.model.properties.Select;
import br.demo.backend.model.relations.PropertyValue;
import br.demo.backend.model.tasks.Log;
import br.demo.backend.model.tasks.Task;
import br.demo.backend.repository.ProjectRepository;
import br.demo.backend.repository.pages.OrderedPageRepository;
import br.demo.backend.repository.pages.PageRepository;
import br.demo.backend.repository.properties.DateRepository;
import br.demo.backend.repository.properties.LimitedRepository;
import br.demo.backend.repository.properties.PropertyRepository;
import br.demo.backend.repository.properties.SelectRepository;
import br.demo.backend.repository.tasks.LogRepository;
import br.demo.backend.service.PropertyValueService;
import br.demo.backend.utils.AutoMapper;
import br.demo.backend.repository.relations.PropertyValueRepository;
import br.demo.backend.repository.tasks.TaskRepository;
import br.demo.backend.service.tasks.TaskService;
import br.demo.backend.utils.IdProjectValidation;
import br.demo.backend.utils.ModelToGetDTO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
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
    private IdProjectValidation validation;
    private AutoMapper<Select> autoMapperSelect;
    private AutoMapper<Date> autoMapperDate;
    private OrderedPageRepository orderedPageRepository;
    private PropertyValueRepository taskValueRepository;
    private PropertyValueService propertyValueService;
    private PropertyValueRepository propertyValueRepository;
    private LogRepository logRepository;

    //that method is used to add the new property to the tasks that already exists
    private void setInTheTasksThatAlreadyExists(Property property) {
        if (property.getPages() != null && !property.getPages().isEmpty()) {
            setRelationAtPage(property, property.getPages());
        } else {
            Project project = projectRepository.findById(property.getProject().getId()).get();
            setRelationAtPage(property, project.getPages());
        }
    }

    private void setRelationAtPage(Property property, Collection<Page> pages) {
        pages.forEach(p -> {
            //get the page from the database
            Page page = pageRepository.findById(p.getId()).get();
            //get the tasks from the page
            page.getTasks().stream().filter(t -> t.getTask().getProperties().stream()
                    .noneMatch(prop -> prop.getProperty().getId().equals(property.getId())))
                    .forEach(tP -> {
                //add the property to the task
                tP.getTask().getProperties().add(propertyValueService.setTaskProperty(property));
                taskService.update(tP.getTask(), false, page.getProject().getId(), false);
            });
        });
    }

    private <T extends Property> T saveGeneric(T property, JpaRepository<T, Long> repo, Long projectId) {
        if(property.getProject() == null){
            Page page = pageRepository.findById(property.getPages().stream().findFirst().get().getId()).get();
            validation.ofObject(projectId, page.getProject());
        }
        T prop = repo.save(property);
        if (!property.getPages().isEmpty() || property.getProject() != null) setInTheTasksThatAlreadyExists(prop);
        return prop;
    }

    public Limited save(Limited property, Long projectId) {
        return saveGeneric(property, limitedRepository, projectId);
    }

    public Date save(Date property, Long projectId) {
        return saveGeneric(property, dateRepository, projectId);
    }

    public Select save(Select property, Long projectId) {
        return saveGeneric(property, selectRepository, projectId);
    }

    private <T extends Property> PropertyGetDTO updateGeneric(T property, Boolean patching,
                                                              AutoMapper<T> autoMapper,
                                                              JpaRepository<T, Long> repo,
                                                              T empty, Long projectId) {
        T old = repo.findById(property.getId()).get();

        if (patching) BeanUtils.copyProperties(old, empty);
        autoMapper.map(property, empty, patching);
        try {
            validation.ofObject(projectId, old.getProject());
        }catch (NullPointerException e){
            try {
                validation.ofObject(projectId, old.getPages().stream().findFirst().get().getProject());
            } catch (NoSuchElementException ignore){}
        }
        T prop = patching ? old : empty;
        autoMapper.map(property, prop, patching, true);
        //this keep the type, pages and project of the property
        prop.setType(old.getType());
        prop.setPages(old.getPages());
        prop.setProject(old.getProject());
        return ModelToGetDTO.tranform(repo.save(prop));
    }

    public PropertyGetDTO update(Limited propertyDTO, Boolean patching, Long projectId) {
        return updateGeneric(propertyDTO, patching, autoMapperLimited, limitedRepository, new Limited(), projectId);
    }

    public PropertyGetDTO update(Date propertyDTO, Boolean patching, Long projectId) {
        return updateGeneric(propertyDTO, patching, autoMapperDate, dateRepository, new Date(), projectId);
    }

    public PropertyGetDTO update(Select propertyDTO, Boolean patching, Long projectId) {
        return updateGeneric(propertyDTO, patching, autoMapperSelect, selectRepository, new Select(), projectId);
    }

    public void delete(Long id, Long projectId, Boolean isDeletingOther) {
        Property property = propertyRepository.findById(id).get();
        if(property.getProject() == null){
            try {
                Page page = pageRepository.findByPropertiesContaining(property).stream().findFirst().get();
                validation.ofObject(projectId, page.getProject());
            }catch (NoSuchElementException ignore){}
        }else{
        validation.ofObject(projectId, property.getProject());
        }
        if (validateCanBeDeleted(property) || isDeletingOther) {
            orderedPageRepository.findAllByPropertyOrdering_Id(property.getId())
                    .forEach(p -> {
                        List<TypeOfProperty> types = getPossibleSubstitutesTypes(p.getType());
                        Property newPropOrd = getOtherProp(p, property, types);
                        if (newPropOrd == null) newPropOrd = getOtherProp(p.getProject(), property, types);
                        p.setPropertyOrdering(newPropOrd);
                        orderedPageRepository.save(p);
                    });
            //this disassociate the property from the tasks
            disassociate(property);
            propertyRepository.deleteById(property.getId());
        } else {
            throw new PropertyCantBeDeletedException();
        }
    }


    public void disassociate(Property property){
        Collection<PropertyValue> allByPropertyId = propertyValueRepository.findAllByProperty_Id(property.getId());
        allByPropertyId.forEach(prop -> {
            Collection<Task> tasks = taskRepository.findTasksByPropertiesContaining(prop);
            tasks.forEach(t -> {
                t.setProperties(new ArrayList<>(t.getProperties().stream().filter(p -> !p.getProperty().getId().equals(property.getId())).toList()));
                taskRepository.save(t);
            });
            Collection<Project> projects = projectRepository.findAllByValuesContaining(prop);
            projects.forEach(t -> {
                t.setValues((new ArrayList<>(t.getValues().stream().filter(p -> !p.getProperty().getId().equals(property.getId())).toList())));
                projectRepository.save(t);

            });
            Collection<Log> logs = logRepository.findAllByValue(prop);
            logs.forEach(t -> {
                logRepository.deleteById(t.getId());
            });
            propertyValueRepository.deleteById(prop.getId());
        });

    }

    private Boolean validateCanBeDeleted(Property property) {
        TypeOfPage typeOfPage = getTypeOfDependetPage(property);
        if (typeOfPage == null) {
            return true;
        }
        List<TypeOfProperty> typesOfProperty = getPossibleSubstitutesTypes(typeOfPage);
        if (property.getProject() != null) {
            return testInProject(typeOfPage, property, typesOfProperty);
        } else {
            return testInPages(typeOfPage, property, property.getPages(), typesOfProperty);
        }
    }

    private List<TypeOfProperty> getPossibleSubstitutesTypes(TypeOfPage type) {
        return switch (type) {
            case KANBAN ->
                    List.of(TypeOfProperty.SELECT, TypeOfProperty.RADIO, TypeOfProperty.CHECKBOX, TypeOfProperty.TAG);
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
        } else return testInPages(typeOfPage, property, project.getPages(), typesOfProperty);
    }

    //thats the method test if exists some property of some types at the pages
    private Boolean testInPages(TypeOfPage typeOfPage, Property property,
                                Collection<Page> pages, List<TypeOfProperty> typesOfProperty) {
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
