import { useEffect, useState } from 'react'
import { porcentajeAcierto } from '../acierto.js'
import { obtenerAciertos } from '../api/cursos.js'
import Icono from '../components/Icono.jsx'
import { EstadoVacio, Hero } from '../components/Pantalla.jsx'
import { DIFICULTADES } from './NuevoProblema.jsx'
import './AciertoProblemas.css'

const ERROR_GENERAL = 'No se pudieron cargar los aciertos. Inténtalo de nuevo.'

function etiquetaDificultad(valor) {
  return DIFICULTADES.find((opcion) => opcion.valor === valor)?.etiqueta ?? valor
}

// HU44-T2: por cada problema del curso, cuántos envíos hubo, cuántos fueron correctos y el porcentaje de acierto. En el
// orden en que llega de admin-api (ordenar por acierto es HU44-T3).
function AciertoProblemas({ id }) {
  // null mientras carga; luego la lista (puede estar vacía).
  const [problemas, setProblemas] = useState(null)
  // '' sin error; si no, el mensaje que se muestra en vez de la tabla.
  const [error, setError] = useState('')
  // Solo un error de red o del servidor se puede reintentar; 403 y 404 no cambian al reintentar.
  const [puedeReintentar, setPuedeReintentar] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)

  useEffect(() => {
    // Al cambiar de curso o salir de la pantalla, la respuesta que llegue tarde se descarta.
    const controlador = new AbortController()

    async function cargar() {
      try {
        const respuesta = await obtenerAciertos(id, controlador.signal)
        if (controlador.signal.aborted) {
          return
        }
        if (respuesta.status === 403 || respuesta.status === 404) {
          setError('No tienes acceso a los datos de este curso o no existe.')
        } else if (respuesta.ok && Array.isArray(respuesta.cuerpo)) {
          setProblemas(respuesta.cuerpo)
        } else {
          // Incluye el 503 de admin-api cuando user-api no responde.
          setError(ERROR_GENERAL)
          setPuedeReintentar(true)
        }
      } catch {
        // fetch solo lanza error sin conexión con el servidor (o al cancelarse, que se ignora).
        if (!controlador.signal.aborted) {
          setError(ERROR_GENERAL)
          setPuedeReintentar(true)
        }
      }
    }

    cargar()
    return () => controlador.abort()
  }, [id, recargas])

  function reintentar() {
    setError('')
    setPuedeReintentar(false)
    setProblemas(null)
    setRecargas(recargas + 1)
  }

  let contenido
  if (error) {
    contenido = (
      <p>
        {error}{' '}
        {puedeReintentar && (
          <button type="button" onClick={reintentar}>
            Reintentar
          </button>
        )}
      </p>
    )
  } else if (problemas === null) {
    contenido = <p>Cargando aciertos…</p>
  } else if (problemas.length === 0) {
    contenido = (
      <EstadoVacio>
        <p>Este curso aún no tiene problemas con envíos.</p>
      </EstadoVacio>
    )
  } else {
    contenido = (
      // Sin enlaces ni botones dentro, la caja que se desplaza necesita foco propio para poder moverla con el teclado.
      <div className="tabla tabla-aciertos" role="region" aria-label="Aciertos por problema" tabIndex={0}>
        <table>
          <caption className="solo-lectores">Porcentaje de acierto de cada problema del curso</caption>
          <thead>
            <tr>
              <th scope="col">Problema</th>
              <th scope="col">Envíos</th>
              <th scope="col">Correctos</th>
              <th scope="col">Acierto</th>
            </tr>
          </thead>
          <tbody>
            {problemas.map((problema) => {
              const porcentaje = porcentajeAcierto(problema.correctos, problema.intentos)
              return (
                <tr key={problema.id}>
                  <td className="acierto-problema">
                    {problema.titulo}
                    <span className="acierto-dificultad">{etiquetaDificultad(problema.dificultad)}</span>
                  </td>
                  <td>{problema.intentos}</td>
                  <td>{problema.correctos}</td>
                  <td>
                    {porcentaje === null ? (
                      'Sin envíos'
                    ) : (
                      <>
                        {porcentaje} %
                        <div className="progreso" aria-hidden="true">
                          <span style={{ '--valor': `${porcentaje}%` }} />
                        </div>
                      </>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    )
  }

  return (
    <main>
      <Hero clase="hero-compacto">
        <p className="hero-volver">
          <a href="/cursos">
            <Icono nombre="atras" />
            Volver a Mis cursos
          </a>
        </p>
        <h1>Aciertos por problema</h1>
      </Hero>
      {contenido}
    </main>
  )
}

export default AciertoProblemas
