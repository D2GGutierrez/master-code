import { useEffect, useState } from 'react'
import { eliminarCurso, listarCursos } from '../api/cursos.js'
import Aviso from '../components/Aviso.jsx'
import Paginacion from '../components/Paginacion.jsx'

const ERROR_GENERAL = 'No se pudieron cargar tus cursos. Inténtalo de nuevo.'
const ERROR_ELIMINAR = 'No se pudo eliminar el curso. Inténtalo de nuevo.'
const MOTIVO_409 = 'No se puede eliminar este curso porque tiene problemas asociados.'
// admin-api devuelve todos los cursos de una vez: la tabla se pagina en el navegador.
const CURSOS_POR_PAGINA = 10

function MisCursos() {
  // null mientras carga; luego la lista (puede estar vacía).
  const [cursos, setCursos] = useState(null)
  const [error, setError] = useState('')
  const [pagina, setPagina] = useState(1)
  // texto: lo que hay en el campo; busqueda: el término ya aplicado al pulsar «Buscar».
  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState('')
  // id del curso cuya fila pide confirmación (solo una a la vez) o null.
  const [confirmando, setConfirmando] = useState(null)
  const [eliminando, setEliminando] = useState(false)
  const [aviso, setAviso] = useState('')
  const [errorEliminar, setErrorEliminar] = useState('')
  // Cambiarlo vuelve a ejecutar el efecto de carga sin tocar la búsqueda aplicada.
  const [recargas, setRecargas] = useState(0)

  useEffect(() => {
    // Si cambia la búsqueda o se sale de la pantalla antes de la respuesta, esa respuesta se descarta.
    let activo = true

    async function cargar() {
      try {
        const { ok, status, cuerpo } = await listarCursos(busqueda)
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
  }, [busqueda, recargas])

  function pedirConfirmacion(id) {
    setAviso('')
    setErrorEliminar('')
    setConfirmando(id)
  }

  async function eliminar(id) {
    setEliminando(true)
    try {
      const { ok, status, cuerpo } = await eliminarCurso(id)
      if (ok) {
        setAviso('Curso eliminado.')
        setPagina(1)
        setRecargas(recargas + 1)
      } else if (status === 409) {
        // El 409 es una regla del negocio, no una falla: va en el aviso informativo, con el texto del servidor.
        setAviso(cuerpo.detail || MOTIVO_409)
      } else {
        setErrorEliminar(ERROR_ELIMINAR)
        // Recarga por si el curso ya no existe (404).
        setRecargas(recargas + 1)
      }
    } catch {
      setErrorEliminar(ERROR_ELIMINAR)
    } finally {
      setEliminando(false)
      setConfirmando(null)
    }
  }

  function aplicarBusqueda(termino) {
    // Vuelve a la página 1: la página actual puede no existir en el nuevo resultado.
    setPagina(1)
    // Con el mismo término no se recarga: el efecto no se volvería a ejecutar.
    if (termino !== busqueda) {
      setCursos(null)
      setError('')
      setBusqueda(termino)
    }
  }

  function buscar(evento) {
    evento.preventDefault()
    aplicarBusqueda(texto.trim())
  }

  function limpiar() {
    setTexto('')
    aplicarBusqueda('')
  }

  // Sin cursos y sin búsqueda no tiene sentido buscar: se muestra solo el estado vacío.
  const mostrarBuscador = busqueda !== '' || (cursos !== null && cursos.length > 0)

  let contenido
  if (error) {
    contenido = <p>{error}</p>
  } else if (cursos === null) {
    contenido = <p>Cargando cursos…</p>
  } else if (cursos.length === 0 && busqueda) {
    contenido = <p>No se encontraron cursos que coincidan con “{busqueda}”.</p>
  } else if (cursos.length === 0) {
    contenido = (
      <p>
        Aún no tienes cursos. Crea tu primer curso para empezar.{' '}
        <a href="/cursos/nuevo">Crear mi primer curso</a>
      </p>
    )
  } else {
    const inicio = (pagina - 1) * CURSOS_POR_PAGINA
    const cursosDeLaPagina = cursos.slice(inicio, inicio + CURSOS_POR_PAGINA)
    contenido = (
      <>
        <p>Total de cursos: {cursos.length}</p>
        <table>
          <thead>
            <tr>
              <th scope="col">Nombre</th>
              <th scope="col">Lenguaje</th>
              <th scope="col">Problemas</th>
              <th scope="col">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cursosDeLaPagina.map((curso) => (
              <tr key={curso.id}>
                <td>{curso.nombre}</td>
                <td>{curso.lenguaje}</td>
                <td>{curso.cantidad_problemas}</td>
                <td>
                  <a href={`/cursos/${curso.id}/editar`}>Editar</a>{' '}
                  <a href={`/cursos/${curso.id}/inscritos`}>Ver inscritos</a>{' '}
                  <a href={`/cursos/${curso.id}/generar`}>Generar con IA</a>{' '}
                  {confirmando === curso.id ? (
                    <>
                      ¿Eliminar “{curso.nombre}”?{' '}
                      <button type="button" disabled={eliminando} onClick={() => eliminar(curso.id)}>
                        Sí, eliminar
                      </button>{' '}
                      <button type="button" disabled={eliminando} onClick={() => setConfirmando(null)}>
                        Cancelar
                      </button>
                    </>
                  ) : (
                    <button type="button" disabled={eliminando} onClick={() => pedirConfirmacion(curso.id)}>
                      Eliminar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <Paginacion
          pagina={pagina}
          hayAnterior={pagina > 1}
          haySiguiente={inicio + CURSOS_POR_PAGINA < cursos.length}
          onCambiar={setPagina}
        />
      </>
    )
  }

  return (
    <main>
      <h1>MASTER CODE · Mis cursos</h1>
      {mostrarBuscador && (
        // Búsqueda por envío (botón o Enter), no en vivo: una petición por búsqueda, no por tecla.
        <form onSubmit={buscar}>
          <label htmlFor="busqueda">Buscar por nombre</label>{' '}
          <input id="busqueda" type="search" value={texto} onChange={(evento) => setTexto(evento.target.value)} />{' '}
          <button type="submit">Buscar</button>{' '}
          {busqueda && (
            <button type="button" onClick={limpiar}>
              Limpiar
            </button>
          )}
        </form>
      )}
      {/* Encima del contenido: siguen a la vista aunque la lista quede vacía tras eliminar. */}
      <Aviso mensaje={aviso} />
      {errorEliminar && <p>{errorEliminar}</p>}
      {contenido}
    </main>
  )
}

export default MisCursos
