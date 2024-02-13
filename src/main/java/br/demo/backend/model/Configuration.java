package br.demo.backend.model;

import br.demo.backend.model.enums.Language;
import br.demo.backend.model.enums.Theme;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tb_configuration")
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private Boolean notifications;

    private String primaryColor;
    private String secondaryColor;
    @Enumerated(value = EnumType.STRING)
    private Theme theme;
    private Integer fontSize;
    @Enumerated(value = EnumType.STRING)
    private Language language;
    private Boolean libras;
    private Boolean textToSound;

    //add, remove or change
    private Boolean notificTasks;
    private Boolean notificAtAddMeInAGroup;
    private Boolean notificWhenChangeMyPermission;
    //when pass a specific number of points (1000, 5000, etc.)
    private Boolean notificMyPointsChange;
    //when i schedule a task or project
    private Boolean notificSchedules;
    //don't understand but can be replaced with deadlines or something like that
    private Boolean notificMylateProject;
    //when i receive a message
    private Boolean notificChats;
    //when ia task than i am responsible receive a comment
    private Boolean notificComments;
}
