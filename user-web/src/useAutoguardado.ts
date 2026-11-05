import { useEffect, useRef, useState } from 'react'
import { ErrorBorrador, guardarBorrador } from './api/borradores.ts'

// HU35-T3: el borrador se guarda solo, 3 s después de la última tecla (antirrebote). No dibuja nada: el indicador de
// «último guardado» es HU35-T4.
export const ESPERA_MS = 3000
export const REINTENTO_MS = 10000
export const MAX_REINTENTOS = 3

export type EstadoAutoguardado = 'inactivo' | 'pendiente' | 'guardando' | 'guardado' | 'error'

// Sin respuesta (red) o con un fallo del servidor vale la pena reintentar; un 4xx (sin acceso, demasiado largo) no
// cambia por reintentar. El 401 lo maneja el cliente.
function esReintentable(error: unknown) {
  return !(error instanceof ErrorBorrador) || error.status >= 500
}

export function useAutoguardado(problemaId: number, codigo: string, codigoInicial: string, habilitado: boolean) {
  const [estado, setEstado] = useState<EstadoAutoguardado>('inactivo')
  const [ultimoGuardado, setUltimoGuardado] = useState<Date | null>(null)
  // Lo que leen los temporizadores y la escucha de visibilidad: siempre lo más reciente, sin depender del render.
  // referencia es lo último guardado (o el código con que se abrió el editor): igual a él, no hay nada que guardar.
  const referencia = useRef(codigoInicial)
  const actual = useRef(codigo)
  const idActual = useRef(problemaId)
  const activo = useRef(habilitado)
  const guardando = useRef(false)
  const cambioDuranteGuardado = useRef(false)
  const reintentos = useRef(0)
  const temporizador = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const desmontado = useRef(false)

  function programar(ms: number) {
    clearTimeout(temporizador.current)
    const idProgramado = idActual.current
    temporizador.current = setTimeout(() => void guardar(idProgramado), ms)
  }

  async function guardar(idProblema: number) {
    temporizador.current = undefined
    const texto = actual.current
    // Nunca dos guardados a la vez: el que está en curso, al terminar, programa el siguiente.
    if (guardando.current || texto === referencia.current) {
      return
    }
    guardando.current = true
    cambioDuranteGuardado.current = false
    setEstado('guardando')
    let ok = true
    let fallo: unknown
    try {
      await guardarBorrador(idProblema, texto)
      referencia.current = texto
      reintentos.current = 0
      setUltimoGuardado(new Date())
    } catch (error) {
      ok = false
      fallo = error
    }
    guardando.current = false
    // El estudiante siguió escribiendo mientras se guardaba: se guarda lo último, con su espera normal.
    if (cambioDuranteGuardado.current && actual.current !== referencia.current) {
      cambioDuranteGuardado.current = false
      reintentos.current = 0
      if (desmontado.current) {
        void guardar(idProblema)
      } else {
        setEstado('pendiente')
        programar(ESPERA_MS)
      }
      return
    }
    if (ok) {
      setEstado('guardado')
      return
    }
    // La referencia no cambió: siguen habiendo cambios sin guardar.
    setEstado('error')
    if (!desmontado.current && esReintentable(fallo) && reintentos.current < MAX_REINTENTOS) {
      reintentos.current += 1
      programar(REINTENTO_MS)
    }
  }

  // Al salir de la pantalla o al ocultar la pestaña no se esperan los 3 s. Mejor esfuerzo: si se cierra el navegador
  // de golpe, la petición puede no llegar.
  function guardarYa() {
    if (!activo.current || actual.current === referencia.current) {
      return
    }
    if (guardando.current) {
      cambioDuranteGuardado.current = true
      return
    }
    clearTimeout(temporizador.current)
    void guardar(idActual.current)
  }

  // Otro problema u otro código de partida: lo anterior ya no sirve de referencia. Va antes del efecto de cambios.
  useEffect(() => {
    referencia.current = codigoInicial
    idActual.current = problemaId
  }, [problemaId, codigoInicial])

  useEffect(() => {
    actual.current = codigo
    activo.current = habilitado
    if (!habilitado) {
      return
    }
    if (guardando.current) {
      cambioDuranteGuardado.current = true
      return
    }
    if (codigo === referencia.current) {
      // Volvió exactamente a lo último guardado: no hay nada pendiente.
      if (temporizador.current !== undefined) {
        clearTimeout(temporizador.current)
        temporizador.current = undefined
        setEstado(ultimoGuardado ? 'guardado' : 'inactivo')
      }
      return
    }
    // Un cambio nuevo reinicia el ciclo normal, también después de errores.
    reintentos.current = 0
    setEstado('pendiente')
    programar(ESPERA_MS)
  }, [codigo, habilitado])

  useEffect(() => {
    // En desarrollo (StrictMode) el efecto se monta dos veces: se vuelve a marcar como montado.
    desmontado.current = false
    function alCambiarVisibilidad() {
      if (document.visibilityState === 'hidden') {
        guardarYa()
      }
    }
    document.addEventListener('visibilitychange', alCambiarVisibilidad)
    return () => {
      document.removeEventListener('visibilitychange', alCambiarVisibilidad)
      desmontado.current = true
      guardarYa()
      clearTimeout(temporizador.current)
    }
  }, [])

  return { estado, ultimoGuardado }
}
