package br.demo.backend.model;

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
@Table(name = "tb_error_log")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 999999999)
    private String exceptionMessage;
    private OffsetDateTime timestamp;
    @Column(length = 999999999)
    private String stackTrace;

    public ErrorLog(Exception e) {
        this.exceptionMessage = e.getMessage();
        this.timestamp = OffsetDateTime.now();
    }

}
