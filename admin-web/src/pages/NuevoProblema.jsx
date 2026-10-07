import { useEffect, useState } from 'react'
import { listarCursos } from '../api/cursos.js'
import { crearProblema } from '../api/problemas.js'
import EditorEnunciado from '../components/EditorEnunciado.jsx'
import Formulario from '../components/Formulario.jsx'

// Valores de las listas fijas que acepta admin-api; la etiqueta es solo para mostrar.
const LENGUAJES = [
  { valor: 'python', etiqueta: 'Python' },
  { valor: 'java', etiqueta: 'Java' },
  { valor: 'javascript', etiqueta: 'JavaScript' },
]
const DIFICULTADES = [
  { valor: 'facil', etiqueta: 'Fácil' },
  { valor: 'medio', etiqueta: 'Medio' },
  { valor: 'dificil', etiqueta: 'Difícil' },
]

// admin-api responde los errores como { campo: ['mensaje', ...] }.
function erroresDelServidor(campos, cuerpo) {
  const errores = {}
  for (const campo of campos) {
    if (cuerpo[campo.nombre]) {
      errores[campo.nombre] = cuerpo[campo.nombre].join(' ')
    }
  }
  if (Object.keys(errores).length === 0) {
    errores.general = 'No se pudo crear el problema. Inténtalo de nuevo.'
  }
  return errores
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

  const campos = [
    {
      nombre: 'curso',
      etiqueta: 'Curso',
      tipo: 'select',
      opciones: (cursos ?? []).map((curso) => ({ valor: String(curso.id), etiqueta: curso.nombre })),
    },
    { nombre: 'titulo', etiqueta: 'Título', tipo: 'text' },
    { nombre: 'enunciado', etiqueta: 'Enunciado', componente: EditorEnunciado },
    { nombre: 'lenguaje', etiqueta: 'Lenguaje de programación', tipo: 'select', opciones: LENGUAJES },
    { nombre: 'dificultad', etiqueta: 'Dificultad', tipo: 'select', opciones: DIFICULTADES },
  ]

  async function crear(valores) {
    // El curso va en la URL, no en el cuerpo.
    const { curso, ...datos } = valores
    setEnviando(true)
    setErrores({})
    try {
      // Si nunca se escribió en el editor no hay enunciado: se envía "" para que el servidor lo rechace.
      const { ok, cuerpo } = await crearProblema(curso, { ...datos, enunciado: datos.enunciado ?? '' })
      if (ok) {
        setCreado(cuerpo.titulo)
      } else {
        setErrores(erroresDelServidor(campos, cuerpo))
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
      <>
        <p>Problema creado: {creado}.</p>
        <p>
          <a href="/problemas/nuevo">Crear otro problema</a> · <a href="/cursos">Ver mis cursos</a>
        </p>
      </>
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
      />
    )
  }

  return (
    <main>
      <h1>MASTER CODE · Nuevo problema</h1>
      {contenido}
    </main>
  )
}

export default NuevoProblema
