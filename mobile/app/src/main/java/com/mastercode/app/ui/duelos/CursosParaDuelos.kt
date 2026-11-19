package com.mastercode.app.ui.duelos

import androidx.annotation.StringRes
import com.mastercode.app.R
import com.mastercode.app.data.cursos.CursoInscrito
import com.mastercode.app.data.cursos.MisCursosRepository
import com.mastercode.app.data.cursos.ResultadoMisCursos

// Los cursos del estudiante, de donde «Retar» y «Ranking» eligen el suyo.
sealed interface ContenidoDeCursos {
    data object Cargando : ContenidoDeCursos
    data class Lista(val cursos: List<CursoInscrito>) : ContenidoDeCursos
    data object Vacio : ContenidoDeCursos
    data class Error(@param:StringRes val mensaje: Int) : ContenidoDeCursos
    data object SesionVencida : ContenidoDeCursos
}

internal suspend fun MisCursosRepository.cursosParaDuelos(): ContenidoDeCursos = when (val resultado = misCursos()) {
    is ResultadoMisCursos.Exito -> if (resultado.cursos.isEmpty()) ContenidoDeCursos.Vacio else ContenidoDeCursos.Lista(resultado.cursos)
    ResultadoMisCursos.SesionVencida -> ContenidoDeCursos.SesionVencida
    ResultadoMisCursos.SinConexion -> ContenidoDeCursos.Error(R.string.duelos_error_conexion)
    ResultadoMisCursos.ErrorInesperado -> ContenidoDeCursos.Error(R.string.duelos_error_inesperado)
}
