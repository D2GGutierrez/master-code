import { useEffect, useState } from 'react'
import { listarCursos } from '../api/cursos.js'

const ERROR_GENERAL = 'No se pudieron cargar tus cursos. Inténtalo de nuevo.'

function MisCursos() {
  // null mientras carga; luego la lista (puede estar vacía).
  const [cursos, setCursos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    // Si se sale de la pantalla antes de la respuesta, no se actualiza un componente desmontado.
    let activo = true

    async function cargar() {
      try {
        const { ok, status, cuerpo } = await listarCursos()
        if (!activo) {
          return
        }
        if (ok) {
          setCursos(cuerpo)
        } else if (status === 401) {
          // Llega aquí solo si el interceptor ya intentó renovar el token y no pudo.
          setError('Tu sesión expiró. Inicia sesión de nuevo.')
        } else {
          setError(ERROR_GENERAL)
        }
      } catch {
        if (activo) {
          setError(ERROR_GENERAL)
        }
      }
    }

    cargar()
    return () => {
      activo = false
    }
  }, [])

  let contenido
  if (error) {
    contenido = <p>{error}</p>
  } else if (cursos === null) {
    contenido = <p>Cargando cursos…</p>
  } else {
    contenido = (
      <table>
        <thead>
          <tr>
            <th scope="col">Nombre</th>
            <th scope="col">Lenguaje</th>
            <th scope="col">Problemas</th>
          </tr>
        </thead>
        <tbody>
          {cursos.map((curso) => (
            <tr key={curso.id}>
              <td>{curso.nombre}</td>
              <td>{curso.lenguaje}</td>
              <td>{curso.cantidad_problemas}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )
  }

  return (
    <main>
      <h1>MASTER CODE · Mis cursos</h1>
      {contenido}
    </main>
  )
}

export default MisCursos
