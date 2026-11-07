import { useEffect, useRef, useState } from 'react'
import { listarCursos } from '../api/cursos.js'
import { crearEvaluacion } from '../api/evaluaciones.js'
import { listarProblemasCurso } from '../api/problemas.js'
import Icono from '../components/Icono.jsx'
import { EstadoVacio, Hero } from '../components/Pantalla.jsx'
import SelectorProblemas from '../components/SelectorProblemas.jsx'
import { aInstante, validarEvaluacion } from '../evaluacion.js'
import '../components/Formulario.css'
import './NuevaEvaluacion.css'

const ERROR_CARGA = 'No se pudo cargar el curso. Inténtalo de nuevo.'
const ERROR_CREAR = 'No se pudo crear la evaluación. Inténtalo de nuevo.'
const SIN_ACCESO = 'No tienes acceso a este curso o no existe.'
// El orden en que se recorren los campos para llevar el foco al primero con error.
const ORDEN_CAMPOS = ['inicio', 'fin', 'limite', 'problemas']
// Nombre en admin-api → nombre en el formulario.
const CAMPOS_SERVIDOR = { fecha_inicio: 'inicio', fecha_fin: 'fin', limite_minutos: 'limite', problemas: 'problemas' }

// admin-api da una lista de mensajes por campo; con un id inválido en problemas puede dar otra forma.
function texto(valor) {
  if (typeof valor === 'string') {
    return valor
  }
  if (Array.isArray(valor) && valor.every((mensaje) => typeof mensaje === 'string')) {
    return valor.join(' ')
  }
  return 'Revisa este dato.'
}

function erroresDelServidor(status, cuerpo) {
  // Llega un 401 solo si el interceptor ya intentó renovar el token y no pudo.
  if (status === 401) {
    return { general: 'Tu sesión expiró. Inicia sesión de nuevo.' }
  }
  if (status === 403 || status === 404) {
    return { general: SIN_ACCESO }
  }
  const errores = {}
  for (const [nombreServidor, nombre] of Object.entries(CAMPOS_SERVIDOR)) {
    if (cuerpo[nombreServidor]) {
      errores[nombre] = texto(cuerpo[nombreServidor])
    }
  }
  const general = cuerpo.detail ?? cuerpo.non_field_errors
  if (general) {
    errores.general = texto(general)
  } else if (Object.keys(errores).length === 0) {
    errores.general = ERROR_CREAR
  }
  return errores
}

// HU61-T3 (CA-045.1 a CA-045.4, CA-046.3): el docente crea una evaluación de su curso con su ventana (inicio y fin), el
// límite en minutos de cada estudiante y los problemas aprobados que contiene. Sin nombre: el modelo no lo tiene.
function NuevaEvaluacion({ cursoId }) {
  // null mientras carga; luego el curso del docente y el banco completo del curso.
  const [curso, setCurso] = useState(null)
  const [banco, setBanco] = useState(null)
  // '' sin error; si no, el mensaje que se muestra en vez del formulario.
  const [errorCarga, setErrorCarga] = useState('')
  // Solo un error de red o del servidor se puede reintentar; 403 y 404 no cambian al reintentar.
  const [puedeReintentar, setPuedeReintentar] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)
  const [valores, setValores] = useState({ inicio: '', fin: '', limite: '' })
  const [seleccionados, setSeleccionados] = useState([])
  const [errores, setErrores] = useState({})
  const [enviando, setEnviando] = useState(false)
  const [creada, setCreada] = useState(false)
  // Un segundo clic en el mismo instante llega antes de que React deshabilite el botón: esto lo frena.
  const enCurso = useRef(false)
  const refInicio = useRef(null)
  const refFin = useRef(null)
  const refLimite = useRef(null)
  const refProblemas = useRef(null)
  const zona = Intl.DateTimeFormat().resolvedOptions().timeZone

  useEffect(() => {
    // Al cambiar de curso o salir de la pantalla, la respuesta que llegue tarde se descarta.
    const controlador = new AbortController()

    async function cargar() {
      try {
        // admin-api no tiene detalle de curso: se busca en la lista de cursos del docente, como en EditarCurso.
        const [cursos, problemas] = await Promise.all([
          listarCursos(),
          listarProblemasCurso(cursoId, controlador.signal),
        ])
        if (controlador.signal.aborted) {
          return
        }
        const encontrado = cursos.ok && Array.isArray(cursos.cuerpo) && cursos.cuerpo.find((c) => c.id === cursoId)
        if (problemas.status === 403 || problemas.status === 404 || (cursos.ok && !encontrado)) {
          setErrorCarga(SIN_ACCESO)
        } else if (encontrado && problemas.ok && Array.isArray(problemas.cuerpo)) {
          setCurso(encontrado)
          setBanco(problemas.cuerpo)
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
    setRecargas(recargas + 1)
  }

  // El error de un campo se quita en cuanto el docente lo cambia.
  function cambiar(evento) {
    const { name, value } = evento.target
    setValores({ ...valores, [name]: value })
    setErrores({ ...errores, [name]: undefined })
  }

  function cambiarSeleccion(ids) {
    setSeleccionados(ids)
    setErrores({ ...errores, problemas: undefined })
  }

  function mostrarErrores(nuevos) {
    setErrores(nuevos)
    const primero = ORDEN_CAMPOS.find((nombre) => nuevos[nombre])
    const campos = { inicio: refInicio, fin: refFin, limite: refLimite, problemas: refProblemas }
    campos[primero]?.current?.focus()
  }

  async function crear(evento) {
    evento.preventDefault()
    if (enCurso.current) {
      return
    }
    const encontrados = validarEvaluacion({ ...valores, problemas: seleccionados })
    if (Object.keys(encontrados).length > 0) {
      mostrarErrores(encontrados)
      return
    }
    enCurso.current = true
    setEnviando(true)
    setErrores({})
    try {
      const { ok, status, cuerpo } = await crearEvaluacion(cursoId, {
        fecha_inicio: aInstante(valores.inicio),
        fecha_fin: aInstante(valores.fin),
        limite_minutos: Number(valores.limite),
        problemas: seleccionados,
      })
      if (ok) {
        setCreada(true)
      } else {
        mostrarErrores(erroresDelServidor(status, cuerpo))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: ERROR_CREAR })
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  // Ayuda y error de un campo, para aria-describedby.
  function describir(nombre, ...otros) {
    const ids = [...otros, errores[nombre] && `${nombre}-error`].filter(Boolean)
    return ids.length > 0 ? ids.join(' ') : undefined
  }

  function error(nombre) {
    return (
      errores[nombre] && (
        <p id={`${nombre}-error`} className="error" role="alert">
          {errores[nombre]}
        </p>
      )
    )
  }

  // Una evaluación solo admite problemas aprobados: el resto no se ofrece y se cuenta aparte.
  const aprobados = (banco ?? []).filter((problema) => problema.aprobado_por_docente === true)

  let contenido
  if (errorCarga) {
    contenido = (
      <p>
        {errorCarga}{' '}
        {puedeReintentar ? (
          <button type="button" onClick={reintentar}>
            Reintentar
          </button>
        ) : (
          <a href="/cursos">Volver a Mis cursos</a>
        )}
      </p>
    )
  } else if (curso === null) {
    contenido = <p>Cargando…</p>
  } else if (creada) {
    contenido = (
      <EstadoVacio ilustracion="exito">
        <p>Evaluación creada.</p>
        <p>
          <a href="/cursos">Ver mis cursos</a> ·{' '}
          <a href={`/cursos/${cursoId}/evaluaciones/nueva`}>Crear otra evaluación</a>
        </p>
      </EstadoVacio>
    )
  } else {
    contenido = (
      <form className="formulario" onSubmit={crear} noValidate>
        {errores.general && (
          <p className="error" role="alert">
            {errores.general}
          </p>
        )}
        <div className="evaluacion-fechas">
          <div className="campo">
            <label htmlFor="inicio">Fecha y hora de inicio</label>
            <input
              ref={refInicio}
              id="inicio"
              name="inicio"
              type="datetime-local"
              value={valores.inicio}
              onChange={cambiar}
              required
              aria-invalid={errores.inicio ? 'true' : undefined}
              aria-describedby={describir('inicio', 'zona-ayuda')}
            />
            {error('inicio')}
          </div>
          <div className="campo">
            <label htmlFor="fin">Fecha y hora de fin</label>
            <input
              ref={refFin}
              id="fin"
              name="fin"
              type="datetime-local"
              value={valores.fin}
              onChange={cambiar}
              required
              aria-invalid={errores.fin ? 'true' : undefined}
              aria-describedby={describir('fin', 'zona-ayuda')}
            />
            {error('fin')}
          </div>
        </div>
        <p id="zona-ayuda" className="campo-ayuda">
          Las fechas usan la hora de tu equipo (zona: {zona}).
        </p>
        <div className="campo">
          <label htmlFor="limite">Límite de tiempo (minutos)</label>
          <input
            ref={refLimite}
            id="limite"
            name="limite"
            type="number"
            min="1"
            step="1"
            inputMode="numeric"
            value={valores.limite}
            onChange={cambiar}
            required
            aria-invalid={errores.limite ? 'true' : undefined}
            aria-describedby={describir('limite', 'limite-ayuda')}
          />
          <p id="limite-ayuda" className="campo-ayuda">
            Minutos que tiene cada estudiante desde que entra a la evaluación.
          </p>
          {error('limite')}
        </div>
        {/* El selector no recibe el foco por sí mismo: lo recibe esta caja cuando falta elegir problemas. */}
        <div ref={refProblemas} className="selector-foco" tabIndex={-1}>
          <SelectorProblemas
            problemas={aprobados}
            seleccionados={seleccionados}
            alCambiar={cambiarSeleccion}
            error={errores.problemas}
            omitidos={banco.length - aprobados.length}
          />
        </div>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Creando…' : 'Crear evaluación'}
        </button>
      </form>
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
        <h1>Nueva evaluación</h1>
        {curso && <p>{curso.nombre}</p>}
      </Hero>
      <div className="panel-formulario panel-formulario-ancho">{contenido}</div>
    </main>
  )
}

export default NuevaEvaluacion
