package br.demo.backend.model.values;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "tb_date_with_google")
public class DateWithGoogle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @EqualsAndHashCode.Include
    private OffsetDateTime dateTime;
    private String idGoogle;
}
