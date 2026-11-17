package com.mastercode.app.ui.dojo

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mastercode.app.R
import com.mastercode.app.data.dojo.DojoRepository
import com.mastercode.app.data.dojo.ResultadoInicioDojo
import com.mastercode.app.data.dojo.ResultadoSiguiente
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
)

// HU53-T3: abre la sesión del Dojo, trae un ejercicio tras otro y cuenta los aciertos. HU55-T1: guarda el estado de la
// sesión (ejercicio actual y aciertos) en cada cambio y al salir de la pantalla.
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
                // La sesión ya no existe o terminó: no hay nada que retomar.
                almacen.guardar(null)
                _uiState.value = DojoUiState(fase = FaseDelDojo.Error(R.string.dojo_error_sesion))
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
