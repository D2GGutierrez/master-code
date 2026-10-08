package com.mastercode.userapi.perfil;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    // ON CONFLICT en vez de "buscar y luego insertar": si llegan dos peticiones a la vez
    // con el mismo email, la segunda no falla con el UNIQUE ni crea otra fila.
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO app_schema.app_estudiante (email, institucion) VALUES (:email, :institucion)"
            + " ON CONFLICT (email) DO NOTHING", nativeQuery = true)
    void insertarSiNoExiste(String email, String institucion);

    Optional<Estudiante> findByEmail(String email);
}
