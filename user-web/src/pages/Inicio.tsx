import { useEffect, useState } from 'react'
import { misCursos, type MiCurso } from '../api/cursos.ts'
import { etiquetaLenguaje } from '../lenguajes.ts'
import './Inicio.css'

// Toda la tarjeta es el enlace a los problemas del curso (HU29-T2).
function TarjetaCurso({ curso }: { curso: MiCurso }) {
  return (
    <li className="tarjeta-curso">
      <a className="tarjeta-curso-enlace" href={`/cursos/${curso.curso_id}/problemas`}>
        <h2>{curso.nombre}</h2>
        <p>{etiquetaLenguaje(curso.lenguaje)}</p>
        <p>Inscrito el {new Date(curso.fecha_inscripcion).toLocaleDateString('es-PE')}</p>
      </a>
    </li>
  )
}

function Inicio() {
  const [cursos, setCursos] = useState<MiCurso[] | null>(null)
  const [error, setError] = useState(false)

  function cargar() {
    misCursos().then(setCursos, () => setError(true))
  }

  function reintentar() {
    setError(false)
    cargar()
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    cargar()
  }, [])

  return (
    <main className="inicio">
      <h1>Mis cursos</h1>
      <p>
        <a href="/cursos/buscar">Buscar cursos</a>
      </p>
      {error ? (
        <>
          <p role="alert">No se pudieron cargar tus cursos. Inténtalo de nuevo.</p>
          <button type="button" onClick={reintentar}>Reintentar</button>
        </>
      ) : !cursos ? (
        <p>Cargando tus cursos…</p>
      ) : cursos.length === 0 ? (
        <p>Todavía no estás inscrito en ningún curso.</p>
      ) : (
        <ul className="inicio-cursos">
          {cursos.map((curso) => (
            <TarjetaCurso key={curso.curso_id} curso={curso} />
          ))}
        </ul>
      )}
    </main>
  )
}

export default Inicio
