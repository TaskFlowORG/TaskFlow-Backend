package br.demo.backend.model.values;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name="tb_date_timelines")
class DateTimelines {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private OffsetDateTime date;

    public DateTimelines(DateTimelines dateTimelines) {
        this.id = null;
        this.date = dateTimelines.getDate();
    }
}
