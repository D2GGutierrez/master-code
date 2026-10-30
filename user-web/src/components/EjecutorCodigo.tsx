import { useId, useState } from 'react'
import { ejecutarCodigo, type ResultadoEjecucion } from '../api/ejecucion.ts'
import EditorCodigo from './EditorCodigo.tsx'
import './EjecutorCodigo.css'

type Props = {
  valor: string
  onCambio: (valor: string) => void
  lenguaje?: string
  altura?: string
}

const ERROR_SERVICIO = 'No se pudo ejecutar el código en este momento. Inténtalo más tarde.'

function mensajeDeError(status: number, detail?: string) {
  if (status === 429) {
    return 'Estás ejecutando demasiado seguido. Espera un momento e inténtalo de nuevo.'
  }
  if (status === 400) {
    return detail ?? 'No se pudo ejecutar el código. Revisa lo que escribiste.'
  }
  return ERROR_SERVICIO
}

// Hasta HU6-T3 el botón no se bloquea: un doble clic envía dos peticiones.
function EjecutorCodigo({ valor, onCambio, lenguaje, altura }: Props) {
  const idEntrada = useId()
  const [entrada, setEntrada] = useState('')
  // Con mensaje se muestra el mensaje; si no, el resultado; si tampoco hay, el texto inicial.
  const [mensaje, setMensaje] = useState('')
  const [resultado, setResultado] = useState<ResultadoEjecucion | null>(null)

  async function ejecutar() {
    setResultado(null)
    if (!valor.trim()) {
      setMensaje('Escribe código antes de ejecutar.')
      return
    }
    setMensaje('')
    try {
      // La entrada va tal cual: los espacios y saltos de línea son parte de lo que lee el programa.
      const respuesta = await ejecutarCodigo({ codigo: valor, lenguaje: lenguaje ?? '', entrada })
      if (respuesta.ok) {
        setResultado(respuesta.resultado)
      } else {
        setMensaje(mensajeDeError(respuesta.status, respuesta.detail))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setMensaje(ERROR_SERVICIO)
    }
  }

  return (
    <div className="ejecutor">
      <div className="editor-caja">
        <EditorCodigo valor={valor} onCambio={onCambio} lenguaje={lenguaje} altura={altura} />
      </div>
      <label htmlFor={idEntrada}>Entrada (opcional)</label>
      <textarea id={idEntrada} rows={3} value={entrada} onChange={(evento) => setEntrada(evento.target.value)} />
      <button type="button" onClick={ejecutar}>
        Ejecutar
      </button>
      <div className="ejecutor-salida" role="region" aria-label="Salida de la ejecución" aria-live="polite">
        {mensaje ? (
          <p>{mensaje}</p>
        ) : !resultado ? (
          <p>Aquí verás la salida de tu código.</p>
        ) : (
          <>
            {resultado.salida ? (
              <pre>{resultado.salida}</pre>
            ) : (
              resultado.estado === 'ok' && <p>El programa no produjo salida.</p>
            )}
            {resultado.error && (
              <>
                <h2 className="ejecutor-error">Error</h2>
                <pre className="ejecutor-error">{resultado.error}</pre>
              </>
            )}
            <p className="ejecutor-estado">
              Estado: {resultado.estado}
              {resultado.tiempo_ms !== null && ` · ${resultado.tiempo_ms} ms`}
            </p>
          </>
        )}
      </div>
    </div>
  )
}

export default EjecutorCodigo
