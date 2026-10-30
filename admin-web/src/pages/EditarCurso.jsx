import { useEffect, useState } from 'react'
import { actualizarCurso, listarCursos } from '../api/cursos.js'
import Aviso from '../components/Aviso.jsx'
import Confirmacion from '../components/Confirmacion.jsx'
import Formulario from '../components/Formulario.jsx'
import Icono from '../components/Icono.jsx'
import { Hero } from '../components/Pantalla.jsx'
import { CAMPOS } from './NuevoCurso.jsx'

const ERROR_GUARDAR = 'No se pudo guardar el curso. Inténtalo de nuevo.'
const LENGUAJES = CAMPOS.find((campo) => campo.nombre === 'lenguaje').opciones

function etiquetaLenguaje(valor) {
  return LENGUAJES.find((opcion) => opcion.valor === valor).etiqueta
}

// Una línea por campo que cambió respecto al curso cargado; vacía si no cambió nada.
function cambios(curso, valores) {
  const lista = []
  // admin-api guarda el nombre sin espacios sobrantes: «Python » no es un cambio.
  if (valores.nombre.trim() !== curso.nombre) {
    lista.push(`Nombre: “${curso.nombre}” → “${valores.nombre.trim()}”`)
  }
  if (valores.lenguaje !== curso.lenguaje) {
    lista.push(`Lenguaje: ${etiquetaLenguaje(curso.lenguaje)} → ${etiquetaLenguaje(valores.lenguaje)}`)
  }
  return lista
}

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
  // Valores del formulario al pulsar «Guardar cambios»; mientras no sean null, el cuadro está abierto.
  const [pendientes, setPendientes] = useState(null)
  const [sinCambios, setSinCambios] = useState(false)

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

  function revisar(valores) {
    const hayCambios = cambios(curso, valores).length > 0
    setSinCambios(!hayCambios)
    if (hayCambios) {
      setPendientes(valores)
    }
  }

  async function guardar() {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, status, cuerpo } = await actualizarCurso(id, pendientes)
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
    } finally {
      setPendientes(null)
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
      <>
        <Aviso mensaje={sinCambios ? 'No hay cambios para guardar.' : ''} />
        <Formulario
          campos={CAMPOS}
          textoBoton="Guardar cambios"
          enviando={enviando}
          errores={errores}
          onEnviar={revisar}
          valoresIniciales={{ nombre: curso.nombre, lenguaje: curso.lenguaje }}
          onCambio={() => setSinCambios(false)}
        />
        <Confirmacion
          abierto={pendientes !== null}
          titulo="¿Guardar los cambios del curso?"
          textoConfirmar="Sí, guardar"
          textoCancelar="Seguir editando"
          enviando={enviando}
          onConfirmar={guardar}
          onCancelar={() => setPendientes(null)}
        >
          {pendientes && (
            <ul>
              {cambios(curso, pendientes).map((linea) => (
                <li key={linea}>{linea}</li>
              ))}
            </ul>
          )}
        </Confirmacion>
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
        <h1>Editar curso</h1>
        {curso && <p>{curso.nombre}</p>}
      </Hero>
      <div className="panel-formulario">{contenido}</div>
    </main>
  )
}

export default EditarCurso
