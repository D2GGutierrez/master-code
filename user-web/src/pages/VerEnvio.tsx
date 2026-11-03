import { useEffect, useState } from 'react'
import { obtenerEnvio, type EnvioDetalle } from '../api/envios.ts'
import { obtenerProblema, type ProblemaDetalle } from '../api/problemas.ts'
import EditorCodigo from '../components/EditorCodigo.tsx'
import Icono from '../components/Icono.tsx'
import { Hero } from '../components/Pantalla.tsx'
import { etiquetaResultado } from '../etiquetas.ts'
import { formatearFecha, formatearPuntaje } from '../historial.ts'
import { claveLenguaje } from '../lenguajes.ts'
import './VerEnvio.css'

// 403 y 404 tienen su mensaje y no se reintentan; cualquier otro fallo permite «Reintentar».
function mensajeDeError(status: number) {
  if (status === 403) {
    return 'No tienes acceso a este envío.'
  }
  if (status === 404) {
    return 'El envío no existe.'
  }
  return ''
}

// HU42-T3 (decisión D8): el código de un envío anterior, en otro editor y solo para leer. No toca el borrador ni el
// código del editor del problema. Sin ejecutor, sin envío, sin copiar al editor (HU42-T4) ni el detalle por caso.
function VerEnvio({ id, envioId }: { id: number; envioId: number }) {
  const [problema, setProblema] = useState<ProblemaDetalle | null>(null)
  const [envio, setEnvio] = useState<EnvioDetalle | null>(null)
  const [sinAcceso, setSinAcceso] = useState('')
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)

  function reintentar() {
    setError(false)
    setRecargas(recargas + 1)
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    // Al cambiar de ids o desmontar, lo que llegue tarde se descarta.
    const controlador = new AbortController()

    function fallo(status: number) {
      if (mensajeDeError(status)) {
        setSinAcceso(mensajeDeError(status))
      } else {
        setError(true)
      }
    }

    // El problema da el título y el lenguaje; si no está disponible, gana su error sobre el del envío.
    async function cargar() {
      const [respuestaProblema, respuestaEnvio] = await Promise.all([
        obtenerProblema(id),
        obtenerEnvio(id, envioId, controlador.signal),
      ])
      if (controlador.signal.aborted) {
        return
      }
      if (!respuestaProblema.ok) {
        fallo(respuestaProblema.status)
        return
      }
      if (!respuestaEnvio.ok) {
        fallo(respuestaEnvio.status)
        return
      }
      setProblema(respuestaProblema.problema)
      setEnvio(respuestaEnvio.envio)
    }

    cargar().catch(() => {
      if (!controlador.signal.aborted) {
        setError(true)
      }
    })
    return () => controlador.abort()
  }, [id, envioId, recargas])

  const volver = `/problemas/${id}/envios`

  if (sinAcceso || error || !problema || !envio) {
    return (
      <main className="ver-envio">
        <p>
          <a href={volver}>Volver al historial</a>
        </p>
        {sinAcceso ? (
          <p role="alert">{sinAcceso}</p>
        ) : error ? (
          <>
            <p role="alert">No se pudo cargar el envío. Inténtalo de nuevo.</p>
            <button type="button" onClick={reintentar}>Reintentar</button>
          </>
        ) : (
          <p role="status">Cargando envío…</p>
        )}
      </main>
    )
  }

  const numero = envio.numero_intento ?? '—'
  return (
    <main className="ver-envio">
      <Hero clase={`hero-compacto hero-curso leng-${claveLenguaje(problema.lenguaje)}`}>
        <p className="hero-volver">
          <a href={volver}>
            <Icono nombre="atras" />
            Volver al historial
          </a>
        </p>
        <p className="hero-sobretitulo">{problema.titulo}</p>
        <h1>Envío N.º {numero}</h1>
      </Hero>
      {/* Las clases resultado-* (colores del resultado) son las del historial, en HistorialEnvios.css. */}
      <p className="ver-envio-datos">
        <span>{formatearFecha(envio.fecha_envio)}</span>
        <span className={`resultado-${envio.resultado}`}>{etiquetaResultado(envio.resultado)}</span>
        <span>{formatearPuntaje(envio.puntaje)}</span>
        <span className="etiqueta-problema">Solo lectura</span>
      </p>
      {!envio.codigo && <p>Este envío no tiene código guardado.</p>}
      <div className="ver-envio-editor" role="region" aria-label={`Código del envío N.º ${numero}, solo lectura`}>
        <EditorCodigo
          valor={envio.codigo}
          onCambio={() => {}}
          lenguaje={problema.lenguaje}
          soloLectura
          altura="clamp(320px, 65dvh, 800px)"
        />
      </div>
    </main>
  )
}

export default VerEnvio
