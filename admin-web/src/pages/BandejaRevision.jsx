import { useEffect, useState } from 'react'
import { listarPendientes } from '../api/problemas.js'
import Paginacion from '../components/Paginacion.jsx'
import { DIFICULTADES, PLATAFORMAS, TIPOS_EJERCICIO } from './NuevoProblema.jsx'
import './BandejaRevision.css'

// admin-api devuelve todos los pendientes de una vez: la tabla se pagina en el navegador.
const PROBLEMAS_POR_PAGINA = 20
const ERROR_CARGA = 'No se pudo cargar la bandeja. Inténtalo de nuevo.'

// admin-api guarda valores internos (p. ej. escritura_codigo); un valor desconocido se muestra tal cual.
function etiqueta(opciones, valor) {
  return opciones.find((opcion) => opcion.valor === valor)?.etiqueta ?? valor
}

// HU18-T2: solo lectura. Aprobar, descartar y editar son HU19 y HU20; el enunciado no se muestra.
function BandejaRevision() {
  // null mientras carga; luego la lista (puede estar vacía).
  const [problemas, setProblemas] = useState(null)
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)
  const [pagina, setPagina] = useState(1)

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
  } else {
    const inicio = (pagina - 1) * PROBLEMAS_POR_PAGINA
    const problemasDeLaPagina = problemas.slice(inicio, inicio + PROBLEMAS_POR_PAGINA)
    contenido = (
      <>
        <p>
          {problemas.length}{' '}
          {problemas.length === 1 ? 'problema pendiente de revisión' : 'problemas pendientes de revisión'}
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
              </tr>
            ))}
          </tbody>
        </table>
        <Paginacion
          pagina={pagina}
          hayAnterior={pagina > 1}
          haySiguiente={inicio + PROBLEMAS_POR_PAGINA < problemas.length}
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
      {contenido}
    </main>
  )
}

export default BandejaRevision
