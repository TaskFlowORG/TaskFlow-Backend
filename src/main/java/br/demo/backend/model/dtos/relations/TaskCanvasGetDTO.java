package br.demo.backend.model.dtos.relations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskCanvasGetDTO extends TaskPageGetDTO {

    //Patch
    private Double x = 500.0;
    //Patch
    private Double y = 500.0;


}
