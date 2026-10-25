import { useEffect, useRef, useState } from 'react'
import { descartarProblema, listarPendientes } from '../api/problemas.js'
import Aviso from '../components/Aviso.jsx'
import Confirmacion from '../components/Confirmacion.jsx'
import Paginacion from '../components/Paginacion.jsx'
import { DIFICULTADES, PLATAFORMAS, TIPOS_EJERCICIO } from './NuevoProblema.jsx'
import './BandejaRevision.css'

// admin-api devuelve todos los pendientes de una vez: la tabla se pagina en el navegador.
const PROBLEMAS_POR_PAGINA = 20
const ERROR_CARGA = 'No se pudo cargar la bandeja. Inténtalo de nuevo.'
const ERROR_DESCARTAR = 'No se pudo descartar el problema. Inténtalo de nuevo.'

// admin-api guarda valores internos (p. ej. escritura_codigo); un valor desconocido se muestra tal cual.
function etiqueta(opciones, valor) {
  return opciones.find((opcion) => opcion.valor === valor)?.etiqueta ?? valor
}

// HU18-T3: se filtra en el navegador sobre la lista cargada. curso llega como número y el <select> da texto.
function filtrar(problemas, curso) {
  return curso ? problemas.filter((problema) => String(problema.curso) === curso) : problemas
}

// HU18-T2: solo lectura. Aprobar, descartar y editar son HU19 y HU20; el enunciado no se muestra.
function BandejaRevision() {
  // null mientras carga; luego la lista (puede estar vacía).
  const [problemas, setProblemas] = useState(null)
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)
  const [pagina, setPagina] = useState(1)
  // Id del curso elegido como texto; '' = todos los cursos.
  const [cursoFiltro, setCursoFiltro] = useState('')
  // HU20-T3: el problema cuyo descarte se está confirmando (el cuadro es modal: uno a la vez) o null.
  const [aDescartar, setADescartar] = useState(null)
  const [descartando, setDescartando] = useState(false)
  // El estado tarda un render en deshabilitar el botón; la marca bloquea al instante un segundo clic.
  const enCurso = useRef(false)
  const [aviso, setAviso] = useState('')
  const [errorDescartar, setErrorDescartar] = useState('')

  useEffect(() => {
    // Si se sale de la pantalla antes de la respuesta, no se actualiza un componente desmontado.
    let activo = true

    async function cargar() {
      try {
        const { ok, cuerpo } = await listarPendientes()
        if (!activo) {
          return
        }
        if (ok) {
          setProblemas(cuerpo)
        } else {
          setError(true)
        }
      } catch {
        // fetch solo lanza error cuando no hay conexión con el servidor.
        if (activo) {
          setError(true)
        }
      }
    }

    cargar()
    return () => {
      activo = false
    }
  }, [recargas])

  function reintentar() {
    setError(false)
    setProblemas(null)
    setRecargas(recargas + 1)
  }

  // La página actual puede no existir en el resultado filtrado: se vuelve a la 1.
  function cambiarCurso(valor) {
    setCursoFiltro(valor)
    setPagina(1)
    setAviso('')
    setErrorDescartar('')
  }

  function pedirConfirmacion(problema) {
    setAviso('')
    setErrorDescartar('')
    setADescartar(problema)
  }

  // Se quita de la lista ya cargada, sin volver a pedirla. Si la página actual queda vacía, se pasa a la
  // anterior; si el curso filtrado se queda sin problemas, el filtro ya no tiene opción y se quita.
  function quitarDeLaLista(id) {
    const restantes = problemas.filter((problema) => problema.id !== id)
    setProblemas(restantes)
    if (cursoFiltro && !restantes.some((problema) => String(problema.curso) === cursoFiltro)) {
      setCursoFiltro('')
      setPagina(1)
      return
    }
    const ultimaPagina = Math.max(1, Math.ceil(filtrar(restantes, cursoFiltro).length / PROBLEMAS_POR_PAGINA))
    if (pagina > ultimaPagina) {
      setPagina(ultimaPagina)
    }
  }

  async function descartar() {
    if (enCurso.current) {
      return
    }
    enCurso.current = true
    setDescartando(true)
    try {
      const { ok, status, cuerpo } = await descartarProblema(aDescartar.id)
      if (ok) {
        quitarDeLaLista(aDescartar.id)
        setAviso('Problema descartado.')
      } else if (status === 404) {
        quitarDeLaLista(aDescartar.id)
        setErrorDescartar('El problema ya no existe. Se actualizó la lista.')
      } else if (status === 403) {
        setErrorDescartar('No tienes permiso para descartar este problema.')
      } else if ((status === 400 || status === 409) && typeof cuerpo.detail === 'string') {
        // Por ejemplo, un problema que ya se aprobó y está publicado (409).
        setErrorDescartar(cuerpo.detail)
      } else {
        setErrorDescartar(ERROR_DESCARTAR)
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor: el problema sigue en la lista.
      setErrorDescartar(ERROR_DESCARTAR)
    } finally {
      enCurso.current = false
      setDescartando(false)
      setADescartar(null)
    }
  }

  // Cursos con problemas en la lista cargada, sin repetir y ordenados por nombre.
  const cursos = [...new Map((problemas ?? []).map((problema) => [String(problema.curso), problema.curso_nombre]))].sort(
    ([, a], [, b]) => a.localeCompare(b, 'es'),
  )
  const filtrados = problemas && filtrar(problemas, cursoFiltro)
  const filtros = (
    <fieldset>
      <legend>Filtros</legend>
      <label htmlFor="filtro-curso">Curso</label>{' '}
      <select id="filtro-curso" value={cursoFiltro} onChange={(evento) => cambiarCurso(evento.target.value)}>
        <option value="">Todos los cursos</option>
        {cursos.map(([idCurso, nombre]) => (
          <option key={idCurso} value={idCurso}>
            {nombre}
          </option>
        ))}
      </select>{' '}
      {cursoFiltro && (
        <button type="button" onClick={() => cambiarCurso('')}>
          Quitar filtros
        </button>
      )}
    </fieldset>
  )

  let contenido
  if (error) {
    contenido = (
      <p>
        {ERROR_CARGA}{' '}
        <button type="button" onClick={reintentar}>
          Reintentar
        </button>
      </p>
    )
  } else if (problemas === null) {
    contenido = <p>Cargando problemas…</p>
  } else if (problemas.length === 0) {
    contenido = (
      <p>
        No tienes problemas pendientes de revisión. <a href="/cursos">Ir a Mis cursos para generar más con IA</a>
      </p>
    )
  } else if (filtrados.length === 0) {
    contenido = (
      <>
        {filtros}
        <p aria-live="polite">Ningún problema coincide con los filtros.</p>
      </>
    )
  } else {
    const inicio = (pagina - 1) * PROBLEMAS_POR_PAGINA
    const problemasDeLaPagina = filtrados.slice(inicio, inicio + PROBLEMAS_POR_PAGINA)
    contenido = (
      <>
        {filtros}
        <p aria-live="polite">
          {cursoFiltro && filtrados.length !== problemas.length
            ? `${filtrados.length} de ${problemas.length} problemas`
            : `${filtrados.length} ${filtrados.length === 1 ? 'problema pendiente de revisión' : 'problemas pendientes de revisión'}`}
        </p>
        <table>
          <caption>Problemas pendientes de revisión</caption>
          <thead>
            <tr>
              <th scope="col">Título</th>
              <th scope="col">Curso</th>
              <th scope="col">Dificultad</th>
              <th scope="col">Tipo de ejercicio</th>
              <th scope="col">Plataforma</th>
              <th scope="col">Creado</th>
              <th scope="col">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {problemasDeLaPagina.map((problema) => (
              <tr key={problema.id}>
                <td>
                  {problema.titulo}{' '}
                  {/* La bandeja solo trae problemas generados por IA: el distintivo va en todos. */}
                  <span className="distintivo-ia">Generado por IA</span>
                </td>
                <td>{problema.curso_nombre}</td>
                <td>{etiqueta(DIFICULTADES, problema.dificultad)}</td>
                <td>{etiqueta(TIPOS_EJERCICIO, problema.tipo_ejercicio)}</td>
                <td>{etiqueta(PLATAFORMAS, problema.plataforma)}</td>
                <td>{new Date(problema.fecha_creacion).toLocaleDateString('es-PE')}</td>
                <td>
                  <button
                    type="button"
                    aria-label={`Descartar “${problema.titulo}”`}
                    onClick={() => pedirConfirmacion(problema)}
                  >
                    Descartar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <Paginacion
          pagina={pagina}
          hayAnterior={pagina > 1}
          haySiguiente={inicio + PROBLEMAS_POR_PAGINA < filtrados.length}
          onCambiar={setPagina}
        />
      </>
    )
  }

  return (
    <main>
      <h1>MASTER CODE · Bandeja de revisión</h1>
      <p>Estos problemas fueron creados por IA. Ningún estudiante los verá hasta que los apruebes.</p>
      <p>
        <a href="/cursos">Volver a Mis cursos</a>
      </p>
      {/* Encima del contenido: siguen a la vista aunque la lista quede vacía tras descartar. */}
      <Aviso mensaje={aviso} />
      {errorDescartar && <p role="alert">{errorDescartar}</p>}
      {contenido}
      <Confirmacion
        abierto={aDescartar !== null}
        titulo="¿Descartar este problema?"
        textoConfirmar="Sí, descartar"
        textoCancelar="Cancelar"
        enviando={descartando}
        onConfirmar={descartar}
        onCancelar={() => setADescartar(null)}
      >
        {aDescartar && (
          <>
            <p>“{aDescartar.titulo}”</p>
            <p>Saldrá de la bandeja y del catálogo del curso, y ningún estudiante lo verá.</p>
          </>
        )}
      </Confirmacion>
    </main>
  )
}

export default BandejaRevision
