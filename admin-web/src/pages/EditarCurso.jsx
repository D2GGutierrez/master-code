import { useEffect, useState } from 'react'
import { actualizarCurso, listarCursos } from '../api/cursos.js'
import Formulario from '../components/Formulario.jsx'
import { CAMPOS } from './NuevoCurso.jsx'

const ERROR_GUARDAR = 'No se pudo guardar el curso. Inténtalo de nuevo.'

// admin-api responde los errores de validación como { campo: ['mensaje', ...] }.
function erroresDelServidor(cuerpo) {
  const errores = {}
  for (const campo of CAMPOS) {
    if (cuerpo[campo.nombre]) {
      errores[campo.nombre] = cuerpo[campo.nombre].join(' ')
    }
  }
  if (Object.keys(errores).length === 0) {
    errores.general = ERROR_GUARDAR
  }
  return errores
}

function EditarCurso({ id }) {
  // null mientras carga.
  const [curso, setCurso] = useState(null)
  const [errorCarga, setErrorCarga] = useState(false)
  const [noExiste, setNoExiste] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})

  useEffect(() => {
    // Si se sale de la pantalla antes de la respuesta, no se actualiza un componente desmontado.
    let activo = true

    async function cargar() {
      try {
        // admin-api no tiene detalle de curso: se busca en la lista de cursos del docente.
        const { ok, cuerpo } = await listarCursos()
        if (!activo) {
          return
        }
        if (!ok) {
          setErrorCarga(true)
          return
        }
        const encontrado = cuerpo.find((c) => c.id === id)
        if (encontrado) {
          setCurso(encontrado)
        } else {
          setNoExiste(true)
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
  }, [id])

  async function guardar(valores) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, status, cuerpo } = await actualizarCurso(id, valores)
      if (ok) {
        window.location.href = '/cursos'
        // El botón sigue deshabilitado mientras el navegador cambia de página.
        return
      }
      if (status === 404) {
        setNoExiste(true)
      } else if (status === 403) {
        setErrores({ general: 'No tienes permiso para editar este curso.' })
      } else {
        setErrores(erroresDelServidor(cuerpo))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: ERROR_GUARDAR })
    }
    setEnviando(false)
  }

  let contenido
  if (noExiste) {
    contenido = (
      <p>
        El curso no existe o ya no está disponible. <a href="/cursos">Volver a Mis cursos</a>
      </p>
    )
  } else if (errorCarga) {
    contenido = <p>No se pudo cargar el curso. Inténtalo de nuevo.</p>
  } else if (curso === null) {
    contenido = <p>Cargando…</p>
  } else {
    // Formulario toma los valores iniciales solo al montarse: por eso se monta con el curso ya cargado.
    contenido = (
      <Formulario
        campos={CAMPOS}
        textoBoton="Guardar cambios"
        enviando={enviando}
        errores={errores}
        onEnviar={guardar}
        valoresIniciales={{ nombre: curso.nombre, lenguaje: curso.lenguaje }}
      />
    )
  }

  return (
    <main>
      <h1>MASTER CODE · Editar curso</h1>
      {contenido}
    </main>
  )
}

export default EditarCurso
