package br.demo.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tb_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    private String name;
    private String surname;
    private String username;
    private String password;
    private String address;
    //Patch
    @OneToOne(cascade = CascadeType.ALL)
    private Archive picture;
    private String mail;
    private String phone;
    private String description;

    //Patch
    private Integer points;

    //Patch
    @OneToOne(cascade = CascadeType.ALL)
    private Configuration configuration;
    @ManyToMany
    private Collection<Permission> permission;
    public User (Long id){
        this.id = id;
    }
}