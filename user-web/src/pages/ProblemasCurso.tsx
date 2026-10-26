import { useEffect, useState } from 'react'
import { misCursos } from '../api/cursos.ts'
import { listarProblemas, type ProblemaDeLista } from '../api/problemas.ts'
import { etiquetaDificultad, etiquetaTipoEjercicio } from '../etiquetas.ts'
import './ProblemasCurso.css'

// Mensaje según el código de user-api. Un curso que no existe también da 403: nadie está inscrito en él.
function mensajeDeError(status: number) {
  if (status === 403) {
    return 'No estás inscrito en este curso.'
  }
  if (status === 404) {
    return 'El curso no existe o ya no está disponible.'
  }
  return ''
}

// HU29-T2: los problemas aún no se abren (el detalle es HU32); el orden es el del servidor (HU29-T3).
function ProblemasCurso({ id }: { id: number }) {
  const [problemas, setProblemas] = useState<ProblemaDeLista[] | null>(null)
  // null mientras carga; si no está entre «Mis cursos», «Curso».
  const [nombreCurso, setNombreCurso] = useState<string | null>(null)
  // 403 y 404 tienen su mensaje y no se reintentan; cualquier otro fallo permite «Reintentar».
  const [sinAcceso, setSinAcceso] = useState('')
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga de problemas.
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
    listarProblemas(id).then(
      (respuesta) => {
        if (respuesta.ok) {
          setProblemas(respuesta.problemas)
        } else if (mensajeDeError(respuesta.status)) {
          setSinAcceso(mensajeDeError(respuesta.status))
        } else {
          setError(true)
        }
      },
      () => setError(true),
    )
  }, [id, recargas])

  useEffect(() => {
    // La lista de problemas no trae el nombre del curso: sale de «Mis cursos». Si falla, el título es genérico.
    misCursos().then(
      (cursos) => setNombreCurso(cursos.find((curso) => curso.curso_id === id)?.nombre ?? 'Curso'),
      () => setNombreCurso('Curso'),
    )
  }, [id])

  return (
    <main className="problemas-curso">
      <h1>{nombreCurso ?? 'Curso'}</h1>
      <p>Problemas del curso</p>
      <p>
        <a href="/inicio">Volver a Mis cursos</a>
      </p>
      {sinAcceso ? (
        <p role="alert">{sinAcceso}</p>
      ) : error ? (
        <>
          <p role="alert">No se pudieron cargar los problemas. Inténtalo de nuevo.</p>
          <button type="button" onClick={reintentar}>Reintentar</button>
        </>
      ) : !problemas ? (
        <p>Cargando problemas…</p>
      ) : problemas.length === 0 ? (
        <p>Este curso todavía no tiene problemas publicados.</p>
      ) : (
        <ul className="lista-problemas">
          {problemas.map((problema) => (
            <li key={problema.id} className="problema">
              <a className="problema-titulo" href={`/problemas/${problema.id}`}>
                {problema.titulo}
              </a>
              <span className="etiqueta-problema">{etiquetaDificultad(problema.dificultad)}</span>{' '}
              <span className="etiqueta-problema">{etiquetaTipoEjercicio(problema.tipo_ejercicio)}</span>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}

export default ProblemasCurso
