import { useEffect, useState } from 'react'
import { listarCursos, listarInscritos } from '../api/cursos.js'
import Paginacion from '../components/Paginacion.jsx'

// admin-api devuelve todos los inscritos de una vez: la tabla se pagina en el navegador.
const INSCRITOS_POR_PAGINA = 20

// Si no es una fecha válida se muestra el texto tal cual llegó.
function formatearFecha(texto) {
  const fecha = new Date(texto)
  return Number.isNaN(fecha.getTime()) ? texto : fecha.toLocaleDateString('es-PE')
}

function EstudiantesInscritos({ id }) {
  // null mientras carga; luego la lista (puede estar vacía).
  const [inscritos, setInscritos] = useState(null)
  const [nombreCurso, setNombreCurso] = useState('')
  // '' sin error; si no, el mensaje que se muestra en vez de la tabla.
  const [error, setError] = useState('')
  // Solo un error de red o del servidor se puede reintentar; 403 y 404 no cambian al reintentar.
  const [puedeReintentar, setPuedeReintentar] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)
  const [pagina, setPagina] = useState(1)

  useEffect(() => {
    // Si se sale de la pantalla antes de la respuesta, no se actualiza un componente desmontado.
    let activo = true

    async function cargar() {
      try {
        // La respuesta de inscritos no trae el nombre del curso: se busca en la lista de cursos del docente.
        const [respuestaInscritos, respuestaCursos] = await Promise.all([listarInscritos(id), listarCursos()])
        if (!activo) {
          return
        }
        if (respuestaInscritos.status === 404) {
          setError('El curso no existe o ya no está disponible.')
        } else if (respuestaInscritos.status === 403) {
          setError('No tienes permiso para ver los inscritos de este curso.')
        } else if (respuestaInscritos.ok && respuestaCursos.ok) {
          setNombreCurso(respuestaCursos.cuerpo.find((curso) => curso.id === id)?.nombre ?? '')
          setInscritos(respuestaInscritos.cuerpo)
        } else {
          // Incluye el 503 de admin-api cuando user-api no responde.
          setError('No se pudieron cargar los estudiantes. Inténtalo de nuevo.')
          setPuedeReintentar(true)
        }
      } catch {
        // fetch solo lanza error cuando no hay conexión con el servidor.
        if (activo) {
          setError('No se pudieron cargar los estudiantes. Inténtalo de nuevo.')
          setPuedeReintentar(true)
        }
      }
    }

    cargar()
    return () => {
      activo = false
    }
  }, [id, recargas])

  function reintentar() {
    setError('')
    setPuedeReintentar(false)
    setInscritos(null)
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
  } else if (inscritos === null) {
    contenido = <p>Cargando estudiantes…</p>
  } else if (inscritos.length === 0) {
    contenido = <p>Aún no hay estudiantes inscritos en este curso.</p>
  } else {
    const inicio = (pagina - 1) * INSCRITOS_POR_PAGINA
    const inscritosDeLaPagina = inscritos.slice(inicio, inicio + INSCRITOS_POR_PAGINA)
    contenido = (
      <>
        <p>
          {inscritos.length} {inscritos.length === 1 ? 'estudiante inscrito' : 'estudiantes inscritos'}
        </p>
        <table>
          <caption>Estudiantes inscritos en {nombreCurso}</caption>
          <thead>
            <tr>
              <th scope="col">Nombre</th>
              <th scope="col">Correo</th>
              <th scope="col">Fecha de inscripción</th>
            </tr>
          </thead>
          <tbody>
            {inscritosDeLaPagina.map((inscrito) => (
              // user-api guarda una sola inscripción por estudiante y curso: el correo no se repite.
              <tr key={inscrito.email}>
                <td>{inscrito.nombre}</td>
                <td>{inscrito.email}</td>
                <td>{formatearFecha(inscrito.fecha_inscripcion)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <Paginacion
          pagina={pagina}
          hayAnterior={pagina > 1}
          haySiguiente={inicio + INSCRITOS_POR_PAGINA < inscritos.length}
          onCambiar={setPagina}
        />
      </>
    )
  }

  return (
    <main>
      <h1>MASTER CODE · Estudiantes inscritos</h1>
      {nombreCurso && <p>{nombreCurso}</p>}
      {contenido}
      <p>
        <a href="/cursos">Volver a Mis cursos</a>
      </p>
    </main>
  )
}

export default EstudiantesInscritos
