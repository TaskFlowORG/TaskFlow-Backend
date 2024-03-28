package br.demo.backend.model.dtos.tasks;


import br.demo.backend.model.dtos.chat.get.MessageGetDTO;
import br.demo.backend.model.dtos.relations.TaskValueGetDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TaskGetDTO {
    @EqualsAndHashCode.Include
    private Long id;
    private String name;

    private Boolean deleted;
    private Boolean completed;

    private Collection<TaskValueGetDTO> properties;

    private Collection<LogGetDTO> logs;


    private Collection<MessageGetDTO> comments;

}
