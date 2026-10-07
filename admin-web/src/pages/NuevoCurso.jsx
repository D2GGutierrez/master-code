import { useState } from 'react'
import { crearCurso } from '../api/cursos.js'
import Formulario from '../components/Formulario.jsx'

// Los valores son la lista fija que acepta admin-api; la etiqueta es solo para mostrar.
const CAMPOS = [
  { nombre: 'nombre', etiqueta: 'Nombre del curso', tipo: 'text' },
  {
    nombre: 'lenguaje',
    etiqueta: 'Lenguaje de programación',
    tipo: 'select',
    opciones: [
      { valor: 'python', etiqueta: 'Python' },
      { valor: 'java', etiqueta: 'Java' },
      { valor: 'javascript', etiqueta: 'JavaScript' },
    ],
  },
]

// admin-api responde los errores como { campo: ['mensaje', ...] }.
function erroresDelServidor(status, cuerpo) {
  // Llega un 401 solo si el interceptor ya intentó renovar el token y no pudo.
  if (status === 401) {
    return { general: 'Tu sesión expiró. Inicia sesión de nuevo.' }
  }
  const errores = {}
  for (const campo of CAMPOS) {
    if (cuerpo[campo.nombre]) {
      errores[campo.nombre] = cuerpo[campo.nombre].join(' ')
    }
  }
  if (Object.keys(errores).length === 0) {
    errores.general = 'No se pudo crear el curso. Inténtalo de nuevo.'
  }
  return errores
}

function NuevoCurso() {
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})
  // Nombre del curso creado, tal como lo guardó el servidor; vacío mientras no se crea.
  const [creado, setCreado] = useState('')

  async function crear(valores) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, status, cuerpo } = await crearCurso(valores)
      if (ok) {
        setCreado(cuerpo.nombre)
      } else {
        setErrores(erroresDelServidor(status, cuerpo))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo crear el curso. Inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  return (
    <main>
      <h1>MASTER CODE · Nuevo curso</h1>
      {creado ? (
        <>
          <p>Curso creado: {creado}.</p>
          <p>
            <a href="/cursos">Ver mis cursos</a> · <a href="/cursos/nuevo">Crear otro curso</a>
          </p>
        </>
      ) : (
        <Formulario
          campos={CAMPOS}
          textoBoton="Crear curso"
          enviando={enviando}
          errores={errores}
          onEnviar={crear}
        />
      )}
    </main>
  )
}

export default NuevoCurso
