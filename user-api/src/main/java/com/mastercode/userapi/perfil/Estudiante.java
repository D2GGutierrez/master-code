package com.mastercode.userapi.perfil;

import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Solo lectura desde Java: la fila la crea EstudianteRepository.insertarSiNoExiste.
@Entity
@Table(name = "app_estudiante")
public class Estudiante {

    @Id
    private Long id;
    private String email;
    private String institucion;
    private OffsetDateTime fechaCreacion;

    public Long getId() {
        return id;
    }
}
