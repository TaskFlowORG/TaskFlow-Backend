package br.demo.backend.service.tasks;



import br.demo.backend.model.tasks.Log;
import br.demo.backend.repository.values.UserValuedRepository;
import br.demo.backend.service.LogService;
import br.demo.backend.service.UserService;
import br.demo.backend.utils.ModelToGetDTO;
import br.demo.backend.model.chat.Message;
import br.demo.backend.model.enums.TypeOfNotification;
import br.demo.backend.service.NotificationService;
import br.demo.backend.model.Project;
import br.demo.backend.model.User;
import br.demo.backend.model.dtos.tasks.TaskGetDTO;
import br.demo.backend.model.enums.Action;
import br.demo.backend.model.enums.TypeOfPage;
import br.demo.backend.model.enums.TypeOfProperty;
import br.demo.backend.model.pages.CanvasPage;
import br.demo.backend.model.pages.OrderedPage;
import br.demo.backend.model.pages.Page;
import br.demo.backend.model.properties.Date;
import br.demo.backend.model.properties.Property;
import br.demo.backend.model.relations.TaskCanvas;
import br.demo.backend.model.relations.TaskOrdered;
import br.demo.backend.model.relations.TaskPage;
import br.demo.backend.model.relations.PropertyValue;
import br.demo.backend.model.tasks.Task;
import br.demo.backend.model.values.*;
import br.demo.backend.repository.ProjectRepository;
import br.demo.backend.repository.UserRepository;
import br.demo.backend.repository.pages.CanvasPageRepository;
import br.demo.backend.repository.pages.OrderedPageRepository;
import br.demo.backend.repository.pages.PageRepository;
import br.demo.backend.repository.relations.TaskPageRepository;
import br.demo.backend.repository.relations.PropertyValueRepository;
import br.demo.backend.repository.tasks.TaskRepository;
import br.demo.backend.utils.AutoMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

@Service
@AllArgsConstructor
public class TaskService {

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private PropertyValueRepository taskValueRepository;
    private PageRepository pageRepositorry;
    private OrderedPageRepository orderedPageRepository;
    private ProjectRepository projectRepository;
    private CanvasPageRepository canvasPageRepository;
    private AutoMapper<Task> autoMapper;
    private NotificationService notificationService;
    private TaskPageRepository taskPageRepository;
    private UserValuedRepository userValuedRepository;
    private LogService logService;
    private UserService userService;


    public TaskGetDTO save(Long idpage) {
        Page page = pageRepositorry.findById(idpage).get();
        Task taskEmpty = taskRepository.save(new Task());

        //add the properties at the task
        addPropertiesAtANewTask(page, taskEmpty);
        //generate the log of create a task
        logService.generateLog(Action.CREATE, taskEmpty);

        Task task = taskRepository.save(taskEmpty);
        //add task to the page setting its type (canvas or ordered)
        addTaskToPage(task, page.getId());
        TaskGetDTO taskGetDTO = ModelToGetDTO.tranform(task);
        //generate the notifications
        notificationService.generateNotification(TypeOfNotification.CHANGETASK, task.getId(), null);
        return taskGetDTO;
    }

    private void addPropertiesAtANewTask(Page page, Task taskEmpty){
        //get the task's page's and task's project's properties
        Collection<Property> propertiesPage = page.getProperties();
        Project project = projectRepository.findByPagesContaining(page);
        Collection<Property> propertiesProject = project.getProperties();

        //set the property values at the task
        taskEmpty.setProperties(new HashSet<>());
        setTaskProperties(propertiesPage, taskEmpty);
        setTaskProperties(propertiesProject, taskEmpty);
    }

    public void addTaskToPage(Task task, Long pageId) {
        Page page = pageRepositorry.findById(pageId).get();
        //this if separate tasks ata tasks at canvas or tasks at other pages
        if(page.getType().equals(TypeOfPage.CANVAS)) {
            page.getTasks().add(new TaskCanvas(null, task, 0.0, 0.0));
            canvasPageRepository.save((CanvasPage) page);
        } else{
            page.getTasks().add(new TaskOrdered(null, task, 0));
             if(page instanceof OrderedPage){
                 orderedPageRepository.save((OrderedPage) page);
             }else{
                 pageRepositorry.save(page);
             }
        }
    }


    //this pass for all the properties to set correctly them at the task
    public void setTaskProperties(Collection<Property> properties, Task taskEmpty) {
        Collection<PropertyValue> propertyValues = new ArrayList<>(properties.stream().map(this::setTaskProperty).toList());
        propertyValues.addAll(taskEmpty.getProperties());
        taskEmpty.setProperties(propertyValues);
        System.out.println(ModelToGetDTO.tranform(taskEmpty));
    }

    //this set the correctly type of value at the propertyvalue
    public PropertyValue setTaskProperty(Property p) {
        Value value= switch (p.getType()) {
            case RADIO, SELECT -> new UniOptionValued();
            case CHECKBOX, TAG -> new MultiOptionValued();
            case TEXT -> new TextValued();
            case DATE -> new DateValued();
            case NUMBER, PROGRESS -> new NumberValued();
            case TIME -> new TimeValued();
            case ARCHIVE -> new ArchiveValued();
            case USER -> new UserValued();
        };
        return new PropertyValue(null, p, value);
    }

    public TaskGetDTO update(Task taskDTO, Boolean patching) {
        Task oldTask = taskRepository.findById(taskDTO.getId()).get();
        Task task = patching ? oldTask : new Task();
        autoMapper.map(taskDTO, task, patching);

        keepFields(task, oldTask);
        //generate logs
        logService.generateLog(Action.UPDATE, task, oldTask);

        TaskGetDTO taskGetDTO = ModelToGetDTO.tranform(taskRepository.save(task));

        //generate the notifications
        notificationService.generateNotification(TypeOfNotification.CHANGETASK, task.getId(), null);
        Collection<Message> comments = task.getComments().stream().filter(c -> !oldTask.getComments().contains(c)).toList();
        comments.forEach(c -> notificationService.generateNotification(TypeOfNotification.COMMENTS, task.getId(), c.getId()));

        return taskGetDTO;
    }

    //this keep the fields that can't be changed
    private void keepFields(Task task, Task oldTask){
        task.setLogs(oldTask.getLogs());
        task.setCompleted(false);
        task.setDeleted(false);
    }

    public void delete(Long id) {
        Task task = taskRepository.findById(id).get();

        //set the attributes to delete the task
        task.setDateDeleted(LocalDateTime.now());
        task.setDeleted(true);

        // generate logs
        logService.generateLog(Action.DELETE, task);
        taskRepository.save(task);
        //generate notifications
        notificationService.generateNotification(TypeOfNotification.CHANGETASK, task.getId(), null);
    }

    public void deletePermanent(Long id) {
        taskPageRepository.deleteAll(taskPageRepository.findAllByTask_Id(id));
        taskRepository.deleteById(id);
    }

    public TaskGetDTO redo(Long id) {
        Task task = taskRepository.findById(id).get();
        //setting the attributes to delete the task
        task.setDeleted(false);
        task.setDateDeleted(null);
        //generate  logs
        logService.generateLog(Action.REDO, task);

        TaskGetDTO tranform = ModelToGetDTO.tranform(taskRepository.save(task));

        //generate notifications
        notificationService.generateNotification(TypeOfNotification.CHANGETASK, task.getId(), null);
        return tranform;
    }

    public Collection<TaskGetDTO> getDeletedTasks(Long projectId){
        Project project = projectRepository.findById(projectId).get();
        Collection<Page> pages = project.getPages();
        //this get all page's tasks of the project and filter by "deleted" attribute
        return pages.stream().map(Page::getTasks).flatMap(Collection::stream)
                .filter(t -> t.getTask().getDeleted()).map(TaskPage::getTask).map(ModelToGetDTO::tranform)
                .distinct().toList();
    }

    public Collection<TaskGetDTO> getTasksToday(String id) {
        User user = userRepository.findByUserDetailsEntity_Username(id).get();
        //get the values that contains the logged user
        Collection<Value> values = userValuedRepository.findAllByUsersContaining(user);
        //get the property values that contains the values
        Collection<PropertyValue> taskValues =
                values.stream().map(v -> taskValueRepository
                                .findByProperty_TypeAndValue(TypeOfProperty.USER, v))
                        .toList();
        //get the tasks that contains the propertyvalues
        Collection<Task> tasks = taskValues.stream().map(tVl -> taskRepository.findByPropertiesContaining(tVl)).toList();
        return tasks.stream().filter(t ->
                        t.getProperties().stream().anyMatch(p ->
                            testIfIsTodayBasesInConfigs(p, user)))
                .map(ModelToGetDTO::tranform).toList();
    }

    private Boolean testIfIsTodayBasesInConfigs(PropertyValue p, User user) {
        if (p.getProperty() instanceof Date property) {
            Boolean deadlineOrScheduling;
            if (user.getConfiguration().getInitialPageTasksPerDeadline()) {
                deadlineOrScheduling = property.getDeadline();
            } else {
                deadlineOrScheduling = property.getScheduling();
            }
            return deadlineOrScheduling && compareToThisDay((LocalDateTime) p.getValue().getValue());
        }
        return false;
    }

    private Boolean compareToThisDay(LocalDateTime time){
        try {
            return time.getMonthValue() == LocalDate.now().getMonthValue() &&
                    time.getYear() == time.getYear() &&
                    time.getDayOfMonth() == time.getDayOfMonth();
        }catch (NullPointerException e){
            return false;
        }
    }

    public TaskGetDTO complete(Long id) {
        Task task = taskRepository.findById(id).get();

        //setting attributes to complete the task
        task.setDateCompleted(LocalDateTime.now());
        task.setCompleted(true);

        //generate the logs and notifications
        logService.generateLog(Action.COMPLETE, task);

        TaskGetDTO tranform = ModelToGetDTO.tranform(taskRepository.save(task));

        generatePointsOfComplete(task.getLogs());

        // generate notifications
        notificationService.generateNotification(TypeOfNotification.CHANGETASK, task.getId(), null);
        return tranform;
    }

    public void generatePointsOfComplete(Collection<Log> logs){
        // Generate the user's points based on alteration at the task
        Collection<User> users = logs.stream().map(Log::getUser).distinct().toList();
        users.forEach(u -> {
            Long qttyLogs = logs.stream().filter(l -> l.getUser().equals(u)).count();
            userService.addPoints(u, qttyLogs * 3);
        });
    }
}
