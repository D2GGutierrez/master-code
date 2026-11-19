import { useEffect, useState } from 'react'
import { listarCursos } from '../api/cursos.js'
import { crearProblema } from '../api/problemas.js'
import { normalizarCodigoBase } from '../codigoBase.js'
import Alternativas from '../components/Alternativas.jsx'
import CasosPrueba from '../components/CasosPrueba.jsx'
import EditorEnunciado from '../components/EditorEnunciado.jsx'
import Formulario from '../components/Formulario.jsx'
import { Dibujo, EstadoVacio, Hero } from '../components/Pantalla.jsx'
import { RespuestaHueco, RespuestaSalida } from '../components/RespuestaEsperada.jsx'
import { faltanteDelEjercicio } from '../completitud.js'

// Valores de las listas fijas que acepta admin-api; la etiqueta es solo para mostrar.
export const LENGUAJES = [
  { valor: 'python', etiqueta: 'Python' },
  { valor: 'java', etiqueta: 'Java' },
  { valor: 'javascript', etiqueta: 'JavaScript' },
]
export const DIFICULTADES = [
  { valor: 'facil', etiqueta: 'Fácil' },
  { valor: 'medio', etiqueta: 'Medio' },
  { valor: 'dificil', etiqueta: 'Difícil' },
]
export const PLATAFORMAS = [
  { valor: 'web', etiqueta: 'Web' },
  { valor: 'movil', etiqueta: 'Móvil' },
  { valor: 'ambas', etiqueta: 'Ambas' },
]
export const TIPOS_EJERCICIO = [
  { valor: 'escritura_codigo', etiqueta: 'Escritura de código' },
  { valor: 'prediccion_salida', etiqueta: 'Predecir la salida' },
  { valor: 'opcion_multiple', etiqueta: 'Opción múltiple' },
  { valor: 'completar_espacio', etiqueta: 'Completar espacio' },
]

// Mismo tope que el campo `tema` de admin-api (H4).
const TEMA_MAX = 60

// H4 (D13): el tema es una etiqueta corta y opcional, por eso no usa el input de Formulario, que siempre es obligatorio.
export function CampoTema({ id, valor, error, onCambio }) {
  const idAyuda = `${id}-ayuda`
  return (
    <>
      <input
        id={id}
        name={id}
        type="text"
        maxLength={TEMA_MAX}
        autoComplete="off"
        value={valor}
        onChange={(evento) => onCambio(evento.target.value)}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${idAyuda} ${id}-error` : idAyuda}
      />
      <p id={idAyuda} className="campo-ayuda">
        Concepto que practica el problema, por ejemplo «bucles» o «listas». Sirve para el resumen de la sesión del
        estudiante; si lo dejas vacío se muestra como «General».
      </p>
    </>
  )
}

// HU34-T2: el código base se escribe como código (monoespaciado, sin ajuste de línea ni correcciones del navegador)
// y es opcional, por eso no usa el input de Formulario, que siempre es obligatorio.
export function CampoCodigoBase({ id, valor, error, onCambio }) {
  const idAyuda = `${id}-ayuda`
  return (
    <>
      <textarea
        id={id}
        name={id}
        className="campo-codigo"
        rows={10}
        wrap="off"
        spellCheck={false}
        autoComplete="off"
        autoCapitalize="off"
        value={valor}
        onChange={(evento) => onCambio(evento.target.value)}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${idAyuda} ${id}-error` : idAyuda}
      />
      <p id={idAyuda} className="campo-ayuda">
        Código que verá el estudiante al abrir el problema, por ejemplo la firma de la función. Déjalo vacío si el
        editor debe empezar en blanco.
      </p>
    </>
  )
}

// admin-api responde los errores como { campo: ['mensaje', ...] }; en casos_prueba puede ser
// una lista con un objeto de errores por caso, que se deja tal cual para CasosPrueba.
export function erroresDelServidor(campos, cuerpo) {
  const errores = {}
  for (const campo of campos) {
    const mensajes = cuerpo[campo.nombre]
    if (mensajes) {
      errores[campo.nombre] = mensajes.every((mensaje) => typeof mensaje === 'string') ? mensajes.join(' ') : mensajes
    }
  }
  if (Object.keys(errores).length === 0) {
    errores.general = 'No se pudo crear el problema. Inténtalo de nuevo.'
  }
  return errores
}

// Los campos del formulario de un problema. Con «cursos» se pide elegir el curso (crear); con null no aparece
// (revisar: el curso de un problema no cambia).
export function camposDelProblema(cursos) {
  return [
    ...(cursos
      ? [
          {
            nombre: 'curso',
            etiqueta: 'Curso',
            tipo: 'select',
            opciones: cursos.map((curso) => ({ valor: String(curso.id), etiqueta: curso.nombre })),
          },
        ]
      : []),
    { nombre: 'titulo', etiqueta: 'Título', tipo: 'text' },
    { nombre: 'enunciado', etiqueta: 'Enunciado', componente: EditorEnunciado },
    { nombre: 'lenguaje', etiqueta: 'Lenguaje de programación', tipo: 'select', opciones: LENGUAJES },
    { nombre: 'dificultad', etiqueta: 'Dificultad', tipo: 'select', opciones: DIFICULTADES },
    { nombre: 'plataforma', etiqueta: 'Plataforma', tipo: 'select', opciones: PLATAFORMAS },
    { nombre: 'tipo_ejercicio', etiqueta: 'Tipo de ejercicio', tipo: 'select', opciones: TIPOS_EJERCICIO },
    {
      nombre: 'casos_prueba',
      etiqueta: 'Casos de prueba',
      componente: CasosPrueba,
      mostrarSi: (valores) => valores.tipo_ejercicio === 'escritura_codigo',
    },
    // Oculto para otros tipos: Formulario solo envía los campos visibles, así que el código escrito no viaja.
    {
      nombre: 'codigo_base',
      etiqueta: 'Código base (opcional)',
      componente: CampoCodigoBase,
      mostrarSi: (valores) => valores.tipo_ejercicio === 'escritura_codigo',
    },
    // H3: cada tipo del celular pide su dato de corrección. Los nombres son los de admin-api (opciones y
    // respuesta_esperada), para que sus errores salgan debajo del campo; lo oculto no se envía.
    {
      nombre: 'opciones',
      etiqueta: 'Alternativas',
      componente: Alternativas,
      mostrarSi: (valores) => valores.tipo_ejercicio === 'opcion_multiple',
    },
    {
      nombre: 'respuesta_esperada',
      etiqueta: 'Salida esperada',
      componente: RespuestaSalida,
      mostrarSi: (valores) => valores.tipo_ejercicio === 'prediccion_salida',
    },
    {
      nombre: 'respuesta_esperada',
      etiqueta: 'Texto que completa el espacio',
      componente: RespuestaHueco,
      mostrarSi: (valores) => valores.tipo_ejercicio === 'completar_espacio',
    },
    { nombre: 'tema', etiqueta: 'Tema (opcional)', componente: CampoTema },
  ]
}

function NuevoProblema() {
  // null mientras cargan los cursos del docente.
  const [cursos, setCursos] = useState(null)
  const [errorCarga, setErrorCarga] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})
  // Título del problema creado, tal como lo guardó el servidor; vacío mientras no se crea.
  const [creado, setCreado] = useState('')

  useEffect(() => {
    // Si se sale de la pantalla antes de la respuesta, no se actualiza un componente desmontado.
    let activo = true

    async function cargar() {
      try {
        const { ok, cuerpo } = await listarCursos()
        if (!activo) {
          return
        }
        if (ok) {
          setCursos(cuerpo)
        } else {
          setErrorCarga(true)
        }
      } catch {
        if (activo) {
          setErrorCarga(true)
        }
      }
    }

    cargar()
    return () => {
      activo = false
    }
  }, [])

  const campos = camposDelProblema(cursos ?? [])

  async function crear(valores) {
    // El curso va en la URL, no en el cuerpo.
    const { curso, ...datos } = valores
    // Solo se descarta un caso con entrada y salida vacías a la vez; los textos van sin recortar.
    const enviados = (datos.casos_prueba || []).filter((caso) => caso.entrada !== '' || caso.salida_esperada !== '')
    if (datos.casos_prueba) {
      datos.casos_prueba = enviados.map(({ entrada, salida_esperada }) => ({ entrada, salida_esperada }))
    }
    // Sin tocar el campo no se envía y admin-api lo guarda vacío.
    if ('codigo_base' in datos) {
      datos.codigo_base = normalizarCodigoBase(datos.codigo_base)
    }
    if ('tema' in datos) {
      datos.tema = datos.tema.trim()
    }
    // H3: las alternativas viajan sin el id de pantalla y la respuesta esperada sin espacios de más en los bordes.
    if (datos.tipo_ejercicio === 'opcion_multiple') {
      datos.opciones = (datos.opciones ?? []).map(({ texto, es_correcta }) => ({ texto: texto.trim(), es_correcta }))
    }
    if ('respuesta_esperada' in datos) {
      datos.respuesta_esperada = datos.respuesta_esperada.trim()
    }
    // admin-api guarda un ejercicio incompleto como borrador sin aprobar y esta pantalla no lo puede completar luego:
    // se avisa aquí, debajo del campo que falta, con la misma regla del servidor.
    const falta = faltanteDelEjercicio({
      tipo: datos.tipo_ejercicio,
      enunciado: datos.enunciado ?? '',
      opciones: datos.opciones,
      respuestaEsperada: datos.respuesta_esperada,
    })
    if (falta) {
      setErrores({ [falta.campo]: falta.mensaje })
      return
    }
    setEnviando(true)
    setErrores({})
    try {
      // Si nunca se escribió en el editor no hay enunciado: se envía "" para que el servidor lo rechace.
      const { ok, cuerpo } = await crearProblema(curso, { ...datos, enunciado: datos.enunciado ?? '' })
      if (ok) {
        setCreado(cuerpo.titulo)
      } else {
        const nuevos = erroresDelServidor(campos, cuerpo)
        // Los errores vienen en el orden de los casos enviados: se ubican en la posición que tiene cada caso en pantalla.
        if (Array.isArray(nuevos.casos_prueba)) {
          nuevos.casos_prueba = valores.casos_prueba.map((caso) => nuevos.casos_prueba[enviados.indexOf(caso)])
        }
        setErrores(nuevos)
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo crear el problema. Inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  let contenido
  if (creado) {
    contenido = (
      <EstadoVacio ilustracion="exito" dibujo="celebracion">
        <p>Problema creado: {creado}.</p>
        <p>
          <a href="/problemas/nuevo">Crear otro problema</a> · <a href="/cursos">Ver mis cursos</a>
        </p>
      </EstadoVacio>
    )
  } else if (errorCarga) {
    contenido = <p>No se pudieron cargar tus cursos. Inténtalo de nuevo.</p>
  } else if (cursos === null) {
    // Mismas clases que Formulario.css, para que se vea como el campo final.
    contenido = (
      <div className="campo">
        <label htmlFor="curso">Curso</label>
        <select id="curso" disabled>
          <option>Cargando cursos…</option>
        </select>
      </div>
    )
  } else if (cursos.length === 0) {
    contenido = (
      <p>
        Primero crea un curso: <a href="/cursos/nuevo">Nuevo curso</a>.
      </p>
    )
  } else {
    contenido = (
      <Formulario
        campos={campos}
        textoBoton="Crear problema"
        enviando={enviando}
        errores={errores}
        onEnviar={crear}
        // admin-api no tiene valor por defecto: se parte de la combinación del flujo actual.
        valoresIniciales={{ plataforma: 'web', tipo_ejercicio: 'escritura_codigo' }}
      />
    )
  }

  return (
    <main>
      <Hero clase="hero-compacto" ilustracion="docente">
        <p className="hero-sobretitulo">Panel docente</p>
        <h1>Nuevo problema</h1>
        <p>Escribe el enunciado y lo que sirve para corregirlo (casos de prueba, alternativas o respuesta esperada) para uno de tus cursos.</p>
      </Hero>
      {creado ? (
        contenido
      ) : (
        <div className="problema-pagina">
          <div className="panel-formulario panel-formulario-ancho">{contenido}</div>
          <aside className="problema-consejos" aria-labelledby="consejos-titulo">
            <Dibujo nombre="equipo" className="problema-consejos-dibujo" />
            <h2 id="consejos-titulo">Antes de crear el problema</h2>
            <ul>
              <li>
                <strong>Enunciado.</strong> Di qué debe hacer el estudiante y, si aplica, qué recibe y qué debe devolver.
              </li>
              <li>
                <strong>Corrección.</strong> Según el tipo de ejercicio, añade casos de prueba, alternativas o la respuesta
                esperada; sin eso el problema no se puede corregir.
              </li>
              <li>
                <strong>Tema.</strong> Una etiqueta corta, como «bucles», agrupa el problema en el resumen de sesión del
                estudiante.
              </li>
              <li>
                <strong>Plataforma.</strong> Elige si lo verán en la web, en el móvil o en ambas.
              </li>
            </ul>
          </aside>
        </div>
      )}
    </main>
  )
}

export default NuevoProblema
