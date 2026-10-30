import { useEffect, useState } from 'react'
import { listarCursos, listarInscritos } from '../api/cursos.js'
import Paginacion from '../components/Paginacion.jsx'
import { aCsv, descargarCsv, nombreArchivoInscritos } from '../csv.js'
import './EstudiantesInscritos.css'
import Icono from '../components/Icono.jsx'
import { EstadoVacio, Hero } from '../components/Pantalla.jsx'

// admin-api devuelve todos los inscritos de una vez: la tabla se pagina en el navegador.
const INSCRITOS_POR_PAGINA = 20

// Si no es una fecha válida se muestra el texto tal cual llegó.
function formatearFecha(texto) {
  const fecha = new Date(texto)
  return Number.isNaN(fecha.getTime()) ? texto : fecha.toLocaleDateString('es-PE')
}

// Mismas columnas y orden que la tabla. La fecha va como AAAA-MM-DD: la parte de fecha de la cadena ISO.
const COLUMNAS_CSV = [
  { encabezado: 'Nombre', valor: (inscrito) => inscrito.nombre },
  { encabezado: 'Correo', valor: (inscrito) => inscrito.email },
  {
    encabezado: 'Fecha de inscripción',
    valor: (inscrito) =>
      Number.isNaN(new Date(inscrito.fecha_inscripcion).getTime())
        ? inscrito.fecha_inscripcion
        : inscrito.fecha_inscripcion.slice(0, 10),
  },
]

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

  // Toda la lista cargada, no solo la página visible de la tabla.
  function descargar() {
    descargarCsv(nombreArchivoInscritos(nombreCurso, id), aCsv(COLUMNAS_CSV, inscritos))
  }

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
    contenido = (
      <EstadoVacio>
        <p>
          Aún no hay estudiantes inscritos en este curso.{' '}
          <button type="button" disabled title="No hay estudiantes para descargar">
            Descargar CSV
          </button>
        </p>
      </EstadoVacio>
    )
  } else {
    const inicio = (pagina - 1) * INSCRITOS_POR_PAGINA
    const inscritosDeLaPagina = inscritos.slice(inicio, inicio + INSCRITOS_POR_PAGINA)
    contenido = (
      <>
        <p>
          {inscritos.length} {inscritos.length === 1 ? 'estudiante inscrito' : 'estudiantes inscritos'}{' '}
          <button type="button" onClick={descargar}>
            Descargar CSV
          </button>
        </p>
        {/* Sin enlaces ni botones dentro, la caja que se desplaza necesita foco propio para poder moverla con el teclado. */}
        <div className="tabla tabla-inscritos" role="region" aria-label="Estudiantes inscritos" tabIndex={0}>
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
        </div>
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
      <Hero clase="hero-compacto">
        <p className="hero-volver">
          <a href="/cursos">
            <Icono nombre="atras" />
            Volver a Mis cursos
          </a>
        </p>
        <h1>Estudiantes inscritos</h1>
        {nombreCurso && <p>{nombreCurso}</p>}
      </Hero>
      {contenido}
    </main>
  )
}

export default EstudiantesInscritos
