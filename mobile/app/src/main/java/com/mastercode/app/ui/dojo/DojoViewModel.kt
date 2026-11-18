package com.mastercode.app.ui.dojo

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.dojo.DojoRepository
import com.mastercode.app.data.dojo.ResultadoEstadoDeSesion
import com.mastercode.app.data.dojo.ResultadoInicioDojo
import com.mastercode.app.data.dojo.ResultadoSiguiente
import com.mastercode.app.data.dojo.ResultadoTerminar
import com.mastercode.app.data.dojo.ResumenDeSesion
import com.mastercode.app.data.problemas.ProblemaDelEjercicio
import com.mastercode.app.data.respuestas.AlmacenDeSesion
import com.mastercode.app.data.respuestas.EstadoDeSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Los lenguajes que acepta POST /api/dojo/sesiones.
val LENGUAJES_DEL_DOJO = listOf("python", "java", "javascript")

// Lo que muestra la pantalla del Dojo: solo uno de estos casos a la vez.
sealed interface FaseDelDojo {
    data object Eligiendo : FaseDelDojo
    data object Iniciando : FaseDelDojo
    data object CargandoEjercicio : FaseDelDojo
    data class Ejercicio(val problema: ProblemaDelEjercicio) : FaseDelDojo
    data object SinMasEjercicios : FaseDelDojo
    data class Error(@param:StringRes val mensaje: Int) : FaseDelDojo
    data object SesionVencida : FaseDelDojo

    // HU55-T2: hay una sesión guardada de la vez anterior; se ofrece continuarla o empezar de nuevo. error es el aviso de
    // un intento que no pudo hacerse (sin conexión), para poder reintentar.
    data class OfreciendoReanudar(val estado: EstadoDeSesion, @param:StringRes val error: Int? = null) : FaseDelDojo

    // HU55-T3: se está cerrando la sesión, y luego el resumen con el que se despide.
    data object Terminando : FaseDelDojo
    data class Terminada(val resumen: ResumenDeSesion) : FaseDelDojo
}

// aciertos es el contador que ve el estudiante (CA-53.4). respondido: el ejercicio de la pantalla ya tiene respuesta
// corregida, así que se puede pasar al siguiente.
data class DojoUiState(
    val fase: FaseDelDojo = FaseDelDojo.Eligiendo,
    val sesionId: Long? = null,
    val lenguaje: String? = null,
    val aciertos: Int = 0,
    val respondidos: Int = 0,
    val respondido: Boolean = false,
    // HU55-T3: la pantalla pregunta «¿Terminar la sesión?» antes de cerrarla; errorAlTerminar es el aviso si no se pudo.
    val confirmandoTermino: Boolean = false,
    @param:StringRes val errorAlTerminar: Int? = null,
)

// HU53-T3: abre la sesión del Dojo, trae un ejercicio tras otro y cuenta los aciertos. HU55-T1: guarda el estado de la
// sesión (ejercicio actual y aciertos) en cada cambio y al salir de la pantalla. HU55-T2: si quedó una guardada, ofrece
// continuarla. HU55-T3: «Terminar sesión» con confirmación y resumen.
class DojoViewModel(
    private val repository: DojoRepository,
    private val almacen: AlmacenDeSesion,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DojoUiState())
    val uiState: StateFlow<DojoUiState> = _uiState.asStateFlow()

    // Los ejercicios ya contados: la pantalla puede avisar dos veces de la misma respuesta (al girar el teléfono) y no
    // debe sumar dos veces.
    private val contados = mutableSetOf<Long>()
    private var ejercicioActualId: Long? = null

    init {
        // Con la sesión guardada sin sesionId en el estado, alSalir no la pisa mientras el estudiante decide.
        almacen.leer()?.let { _uiState.value = DojoUiState(fase = FaseDelDojo.OfreciendoReanudar(it)) }
    }

    // HU55-T2: «Continuar». Se pregunta a user-api cómo va la sesión (no la de la memoria del celular, que pudo quedar vieja)
    // y se muestra el ejercicio que se dejó sin responder; si no había ninguno, se pide el siguiente.
    fun reanudar() {
        val oferta = _uiState.value.fase as? FaseDelDojo.OfreciendoReanudar ?: return
        val guardada = oferta.estado
        _uiState.update { it.copy(fase = FaseDelDojo.Iniciando) }
        viewModelScope.launch {
            when (val resultado = repository.estado(guardada.sesionId)) {
                is ResultadoEstadoDeSesion.Exito -> {
                    val sesion = resultado.estado
                    if (!sesion.abierta) {
                        sesionPerdida()
                        return@launch
                    }
                    contados.clear()
                    ejercicioActualId = sesion.ejercicioActual?.id
                    _uiState.value = DojoUiState(
                        fase = sesion.ejercicioActual?.let { FaseDelDojo.Ejercicio(it) } ?: FaseDelDojo.CargandoEjercicio,
                        sesionId = sesion.id,
                        lenguaje = sesion.lenguaje,
                        aciertos = sesion.aciertos,
                        respondidos = sesion.respondidos,
                    )
                    if (sesion.ejercicioActual == null) pedirSiguiente(sesion.id) else guardarEstado()
                }
                ResultadoEstadoDeSesion.NoExiste -> sesionPerdida()
                ResultadoEstadoDeSesion.SesionVencida -> _uiState.update { it.copy(fase = FaseDelDojo.SesionVencida) }
                ResultadoEstadoDeSesion.SinConexion -> _uiState.update {
                    it.copy(fase = FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_conexion))
                }
                ResultadoEstadoDeSesion.ErrorInesperado -> _uiState.update {
                    it.copy(fase = FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_inesperado))
                }
            }
        }
    }

    // HU55-T2: «Empezar de nuevo». La sesión anterior se termina (así queda guardado el nivel al que llegó) y se elige
    // lenguaje otra vez. Sin conexión no se descarta: se avisa y se puede reintentar.
    fun empezarDeNuevo() {
        val oferta = _uiState.value.fase as? FaseDelDojo.OfreciendoReanudar ?: return
        val guardada = oferta.estado
        _uiState.update { it.copy(fase = FaseDelDojo.Iniciando) }
        viewModelScope.launch {
            when (repository.terminar(guardada.sesionId)) {
                is ResultadoTerminar.Exito, ResultadoTerminar.YaTerminada -> {
                    almacen.guardar(null)
                    _uiState.value = DojoUiState()
                }
                ResultadoTerminar.SesionVencida -> _uiState.update { it.copy(fase = FaseDelDojo.SesionVencida) }
                ResultadoTerminar.SinConexion -> _uiState.update {
                    it.copy(fase = FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_conexion))
                }
                ResultadoTerminar.ErrorInesperado -> _uiState.update {
                    it.copy(fase = FaseDelDojo.OfreciendoReanudar(guardada, R.string.dojo_error_inesperado))
                }
            }
        }
    }

    // La sesión guardada ya no existe o ya terminó: no hay nada que retomar.
    private fun sesionPerdida() {
        almacen.guardar(null)
        _uiState.value = DojoUiState(fase = FaseDelDojo.Error(R.string.dojo_error_sesion))
    }

    // HU55-T3: «Terminar sesión» solo donde hay una sesión quieta (con ejercicio, sin más ejercicios o tras un error).
    private fun sePuedeTerminar(): Boolean {
        val estado = _uiState.value
        return estado.sesionId != null &&
            (estado.fase is FaseDelDojo.Ejercicio || estado.fase == FaseDelDojo.SinMasEjercicios || estado.fase is FaseDelDojo.Error)
    }

    fun pedirTerminar() {
        if (sePuedeTerminar()) _uiState.update { it.copy(confirmandoTermino = true, errorAlTerminar = null) }
    }

    fun cancelarTerminar() {
        _uiState.update { it.copy(confirmandoTermino = false) }
    }

    fun confirmarTerminar() {
        val estado = _uiState.value
        val sesionId = estado.sesionId ?: return
        if (!estado.confirmandoTermino) return
        val faseAntes = estado.fase
        _uiState.update { it.copy(fase = FaseDelDojo.Terminando, confirmandoTermino = false, errorAlTerminar = null) }
        viewModelScope.launch {
            when (val resultado = repository.terminar(sesionId)) {
                is ResultadoTerminar.Exito -> {
                    contados.clear()
                    ejercicioActualId = null
                    almacen.guardar(null)
                    // Sin sesionId: al salir de la pantalla ya no se guarda una sesión cerrada.
                    _uiState.value = DojoUiState(fase = FaseDelDojo.Terminada(resultado.resumen))
                }
                ResultadoTerminar.YaTerminada -> sesionPerdida()
                ResultadoTerminar.SesionVencida -> _uiState.update { it.copy(fase = FaseDelDojo.SesionVencida) }
                ResultadoTerminar.SinConexion -> _uiState.update {
                    it.copy(fase = faseAntes, errorAlTerminar = R.string.dojo_error_conexion)
                }
                ResultadoTerminar.ErrorInesperado -> _uiState.update {
                    it.copy(fase = faseAntes, errorAlTerminar = R.string.dojo_error_inesperado)
                }
            }
        }
    }

    fun elegirLenguaje(lenguaje: String) {
        if (_uiState.value.fase != FaseDelDojo.Eligiendo || lenguaje !in LENGUAJES_DEL_DOJO) return
        _uiState.update { it.copy(fase = FaseDelDojo.Iniciando) }
        viewModelScope.launch {
            when (val resultado = repository.iniciar(lenguaje)) {
                is ResultadoInicioDojo.Exito -> {
                    contados.clear()
                    ejercicioActualId = null
                    _uiState.value = DojoUiState(
                        fase = FaseDelDojo.CargandoEjercicio,
                        sesionId = resultado.sesion.id,
                        lenguaje = resultado.sesion.lenguaje,
                    )
                    guardarEstado()
                    pedirSiguiente(resultado.sesion.id)
                }
                ResultadoInicioDojo.SesionVencida -> _uiState.update { it.copy(fase = FaseDelDojo.SesionVencida) }
                ResultadoInicioDojo.SinConexion -> _uiState.update { it.copy(fase = FaseDelDojo.Error(R.string.dojo_error_conexion)) }
                ResultadoInicioDojo.ErrorInesperado -> _uiState.update { it.copy(fase = FaseDelDojo.Error(R.string.dojo_error_inesperado)) }
            }
        }
    }

    // «Siguiente ejercicio»: solo cuando el actual ya tiene respuesta corregida.
    fun siguiente() {
        val estado = _uiState.value
        val sesionId = estado.sesionId ?: return
        if (estado.fase is FaseDelDojo.Ejercicio && !estado.respondido) return
        _uiState.update { it.copy(fase = FaseDelDojo.CargandoEjercicio, respondido = false) }
        viewModelScope.launch { pedirSiguiente(sesionId) }
    }

    private suspend fun pedirSiguiente(sesionId: Long) {
        when (val resultado = repository.siguiente(sesionId)) {
            is ResultadoSiguiente.Exito -> {
                ejercicioActualId = resultado.problema.id
                _uiState.update { it.copy(fase = FaseDelDojo.Ejercicio(resultado.problema), respondido = false) }
            }
            ResultadoSiguiente.SinMas -> {
                ejercicioActualId = null
                _uiState.update { it.copy(fase = FaseDelDojo.SinMasEjercicios) }
            }
            ResultadoSiguiente.SesionTerminada -> {
                sesionPerdida()
                return
            }
            ResultadoSiguiente.SesionVencida -> _uiState.update { it.copy(fase = FaseDelDojo.SesionVencida) }
            ResultadoSiguiente.SinConexion -> _uiState.update { it.copy(fase = FaseDelDojo.Error(R.string.dojo_error_conexion)) }
            ResultadoSiguiente.ErrorInesperado -> _uiState.update { it.copy(fase = FaseDelDojo.Error(R.string.dojo_error_inesperado)) }
        }
        guardarEstado()
    }

    // Un ejercicio de un tipo que el celular todavía no sabe mostrar: se deja pasar al siguiente sin contarlo como respondido.
    fun omitir() {
        if (_uiState.value.fase is FaseDelDojo.Ejercicio) _uiState.update { it.copy(respondido = true) }
    }

    // La acción de «Reintentar» tras un error: sin sesión abierta se vuelve a elegir lenguaje; con ella, se pide el ejercicio.
    fun alReintentar() {
        val estado = _uiState.value
        if (estado.fase !is FaseDelDojo.Error) return
        val sesionId = estado.sesionId
        if (sesionId == null) {
            _uiState.update { it.copy(fase = FaseDelDojo.Eligiendo) }
        } else {
            _uiState.update { it.copy(fase = FaseDelDojo.CargandoEjercicio) }
            viewModelScope.launch { pedirSiguiente(sesionId) }
        }
    }

    // La pantalla del ejercicio avisa cuando su respuesta ya fue corregida (acertó o no).
    fun alResponder(problemaId: Long, acerto: Boolean) {
        if (!contados.add(problemaId)) return
        _uiState.update {
            it.copy(
                aciertos = it.aciertos + if (acerto) 1 else 0,
                respondidos = it.respondidos + 1,
                respondido = true,
            )
        }
        guardarEstado()
    }

    // HU55-T1: la pantalla avisa al salir (o pasar a segundo plano); queda guardado cuál era el ejercicio y los aciertos.
    fun alSalir() {
        guardarEstado()
    }

    private fun guardarEstado() {
        val estado = _uiState.value
        val sesionId = estado.sesionId ?: return
        val lenguaje = estado.lenguaje ?: return
        almacen.guardar(EstadoDeSesion(sesionId, lenguaje, ejercicioActualId, estado.aciertos, estado.respondidos))
    }

    companion object {
        fun factory(repository: DojoRepository, almacen: AlmacenDeSesion) = viewModelFactory {
            initializer { DojoViewModel(repository, almacen) }
        }
    }
}
