package com.mastercode.app.ui.ejercicios

import kotlinx.coroutines.flow.StateFlow

// Lo que las tres pantallas de ejercicio («predecir la salida», «completar el espacio» y «opción múltiple») necesitan de
// quien las maneja. Lo implementan EjercicioViewModel (problemas sueltos y Dojo) y EjercicioDePartida (duelos, H8): así
// la partida reutiliza las mismas pantallas y solo cambia a dónde se envía la respuesta.
interface ControlDeEjercicio {
    val uiState: StateFlow<EjercicioUiState>

    // La acción de «Reintentar» cuando el ejercicio no cargó.
    fun cargar()

    fun alCambiarRespuesta(texto: String)

    fun alElegirOpcion(opcionId: Long)

    fun enviar()

    fun intentarDeNuevo()
}
