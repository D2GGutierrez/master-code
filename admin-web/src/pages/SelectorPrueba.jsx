import { useEffect, useState } from 'react'
import { listarProblemasCurso } from '../api/problemas.js'
import Icono from '../components/Icono.jsx'
import { Hero } from '../components/Pantalla.jsx'
import SelectorProblemas from '../components/SelectorProblemas.jsx'

const ERROR_CARGA = 'No se pudieron cargar los problemas del curso. Inténtalo de nuevo.'

// Página PROVISIONAL de HU62-T2 para probar el selector: la reemplaza el formulario de la evaluación (HU61-T3), que
// enviará estos ids en «problemas» al crearla (POST /api/cursos/{id}/evaluaciones/). Aquí no se guarda nada.
function SelectorPrueba({ cursoId }) {
  // null mientras carga; luego el banco completo del curso (aprobados y no aprobados).
  const [banco, setBanco] = useState(null)
  // '' sin error; si no, el mensaje que se muestra en vez del selector.
  const [errorCarga, setErrorCarga] = useState('')
  // Solo un error de red o del servidor se puede reintentar; 403 y 404 no cambian al reintentar.
  const [puedeReintentar, setPuedeReintentar] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)
  const [seleccionados, setSeleccionados] = useState([])
  const [errorSeleccion, setErrorSeleccion] = useState('')
  // Lo que el formulario enviaría; null hasta que se comprueba la selección.
  const [comprobados, setComprobados] = useState(null)

  useEffect(() => {
    // Al cambiar de curso o salir de la pantalla, la respuesta que llegue tarde se descarta.
    const controlador = new AbortController()

    async function cargar() {
      try {
        const respuesta = await listarProblemasCurso(cursoId, controlador.signal)
        if (controlador.signal.aborted) {
          return
        }
        if (respuesta.status === 403 || respuesta.status === 404) {
          setErrorCarga('No tienes acceso a este curso o no existe.')
        } else if (respuesta.ok && Array.isArray(respuesta.cuerpo)) {
          setBanco(respuesta.cuerpo)
        } else {
          setErrorCarga(ERROR_CARGA)
          setPuedeReintentar(true)
        }
      } catch {
        // fetch solo lanza error sin conexión con el servidor (o al cancelarse, que se ignora).
        if (!controlador.signal.aborted) {
          setErrorCarga(ERROR_CARGA)
          setPuedeReintentar(true)
        }
      }
    }

    cargar()
    return () => controlador.abort()
  }, [cursoId, recargas])

  function reintentar() {
    setErrorCarga('')
    setPuedeReintentar(false)
    setBanco(null)
    setRecargas(recargas + 1)
  }

  function cambiarSeleccion(ids) {
    setSeleccionados(ids)
    setErrorSeleccion('')
    setComprobados(null)
  }

  // CA-046.3: sin al menos un problema no se llama al servidor.
  function comprobar() {
    if (seleccionados.length === 0) {
      setErrorSeleccion('Selecciona al menos un problema.')
      setComprobados(null)
      return
    }
    setComprobados(seleccionados)
  }

  // Una evaluación solo admite problemas aprobados: el resto no se ofrece y se cuenta aparte.
  const aprobados = (banco ?? []).filter((problema) => problema.aprobado_por_docente === true)

  let contenido
  if (errorCarga) {
    contenido = (
      <p>
        {errorCarga}{' '}
        {puedeReintentar && (
          <button type="button" onClick={reintentar}>
            Reintentar
          </button>
        )}
      </p>
    )
  } else if (banco === null) {
    contenido = <p>Cargando problemas…</p>
  } else {
    contenido = (
      <>
        <SelectorProblemas
          problemas={aprobados}
          seleccionados={seleccionados}
          alCambiar={cambiarSeleccion}
          error={errorSeleccion}
          omitidos={banco.length - aprobados.length}
        />
        <button type="button" onClick={comprobar}>
          Comprobar selección
        </button>
        <p>Ids seleccionados: {seleccionados.length > 0 ? seleccionados.join(', ') : '(ninguno)'}</p>
        {comprobados && <p>Se enviaría: {JSON.stringify({ problemas: comprobados })}</p>}
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
        <h1>Prueba del selector de problemas</h1>
        <p>Página provisional: el formulario de la evaluación (HU61-T3) la reemplazará.</p>
      </Hero>
      {contenido}
    </main>
  )
}

export default SelectorPrueba
