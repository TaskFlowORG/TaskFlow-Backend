package br.demo.backend.model.dtos.chat.get;

import br.demo.backend.model.dtos.user.OtherUsersDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatPrivateGetDTO extends ChatGetDTO {
    private Collection<OtherUsersDTO> users;
}