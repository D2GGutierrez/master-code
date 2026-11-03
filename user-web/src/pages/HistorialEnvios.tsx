import { useEffect, useState } from 'react'
import { listarEnvios, type EnvioResumen } from '../api/envios.ts'
import { obtenerProblema, type ProblemaDetalle } from '../api/problemas.ts'
import Icono from '../components/Icono.tsx'
import { EstadoVacio, Hero } from '../components/Pantalla.tsx'
import { etiquetaResultado } from '../etiquetas.ts'
import { formatearFecha, formatearPuntaje, numeroDeFila } from '../historial.ts'
import { claveLenguaje } from '../lenguajes.ts'
import './HistorialEnvios.css'

// Solo la escritura de código usa el editor (HU32-T3); la misma regla que en DetalleProblema.tsx.
const TIPO_CON_EDITOR = 'escritura_codigo'

// Mensaje según el código de user-api. Hoy responde 404 también si no está inscrito, para no revelar qué existe.
function mensajeDeError(status: number) {
  if (status === 403) {
    return 'No estás inscrito en el curso de este problema.'
  }
  if (status === 404) {
    return 'El problema no existe o no está disponible.'
  }
  return ''
}

// HU41-T2: los envíos del estudiante en un problema, del más reciente al más antiguo, en el orden en que llegan.
// Sin paginación (HU41-T3) ni el código de cada envío (HU42).
function HistorialEnvios({ id }: { id: number }) {
  const [problema, setProblema] = useState<ProblemaDetalle | null>(null)
  const [envios, setEnvios] = useState<EnvioResumen[] | null>(null)
  // 403 y 404 tienen su mensaje y no se reintentan; cualquier otro fallo permite «Reintentar».
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
    // Al cambiar de id o desmontar, lo que llegue tarde se descarta.
    const controlador = new AbortController()

    function fallo(status: number) {
      if (mensajeDeError(status)) {
        setSinAcceso(mensajeDeError(status))
      } else {
        setError(true)
      }
    }

    // Primero el problema (título y acceso), luego sus envíos.
    async function cargar() {
      const respuestaProblema = await obtenerProblema(id)
      if (controlador.signal.aborted) {
        return
      }
      if (!respuestaProblema.ok) {
        fallo(respuestaProblema.status)
        return
      }
      setProblema(respuestaProblema.problema)
      const respuestaEnvios = await listarEnvios(id, controlador.signal)
      if (controlador.signal.aborted) {
        return
      }
      if (respuestaEnvios.ok) {
        setEnvios(respuestaEnvios.envios)
      } else {
        fallo(respuestaEnvios.status)
      }
    }

    cargar().catch(() => {
      if (!controlador.signal.aborted) {
        setError(true)
      }
    })
    return () => controlador.abort()
  }, [id, recargas])

  if (sinAcceso || error || !problema || !envios) {
    return (
      <main className="historial-envios">
        <p>
          <a href={`/problemas/${id}`}>Volver al problema</a>
        </p>
        {sinAcceso ? (
          <p role="alert">{sinAcceso}</p>
        ) : error ? (
          <>
            <p role="alert">No se pudo cargar el historial. Inténtalo de nuevo.</p>
            <button type="button" onClick={reintentar}>Reintentar</button>
          </>
        ) : (
          <p role="status">Cargando tus envíos…</p>
        )}
      </main>
    )
  }

  return (
    <main className="historial-envios">
      <Hero clase={`hero-compacto hero-curso leng-${claveLenguaje(problema.lenguaje)}`}>
        <p className="hero-volver">
          <a href={`/problemas/${id}`}>
            <Icono nombre="atras" />
            Volver al problema
          </a>
        </p>
        <p className="hero-sobretitulo">Mis envíos</p>
        <h1>{problema.titulo}</h1>
      </Hero>
      {envios.length === 0 ? (
        <EstadoVacio>
          <p>Aún no has enviado ninguna solución a este problema.</p>
          {problema.tipo_ejercicio === TIPO_CON_EDITOR && (
            <a href={`/problemas/${id}/editor`}>Ir al editor de código</a>
          )}
        </EstadoVacio>
      ) : (
        // Enfocable: en pantallas angostas se desplaza con las flechas del teclado.
        <div className="tabla tabla-historial" tabIndex={0} role="region" aria-label="Historial de envíos">
          <table>
            <caption className="solo-lectores">Historial de envíos de {problema.titulo}</caption>
            <thead>
              <tr>
                <th scope="col">Intento</th>
                <th scope="col">Fecha</th>
                <th scope="col">Resultado</th>
                <th scope="col">Puntaje</th>
                <th scope="col">Código</th>
              </tr>
            </thead>
            <tbody>
              {envios.map((envio, indice) => {
                const numero = numeroDeFila(envio.numero_intento, indice, envios.length)
                return (
                  <tr key={envio.id}>
                    <td>N.º {numero}</td>
                    <td>{formatearFecha(envio.fecha_envio)}</td>
                    <td className={`resultado-${envio.resultado}`}>{etiquetaResultado(envio.resultado)}</td>
                    <td>{formatearPuntaje(envio.puntaje)}</td>
                    <td>
                      <a href={`/problemas/${id}/envios/${envio.id}`} aria-label={`Ver código del intento ${numero}`}>
                        Ver código
                      </a>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}

export default HistorialEnvios
