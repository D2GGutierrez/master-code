import { useEffect, useRef, useState } from 'react'
import { listarCursos } from '../api/cursos.js'
import { generarProblemas } from '../api/problemas.js'
import Formulario from '../components/Formulario.jsx'
import Icono from '../components/Icono.jsx'
import { Ilustracion } from '../components/Ilustracion.jsx'
import { Hero } from '../components/Pantalla.jsx'
import { CAMPOS as CAMPOS_CURSO } from './NuevoCurso.jsx'
import { DIFICULTADES } from './NuevoProblema.jsx'
import './GenerarProblemas.css'

const ERROR_GENERAR = 'No se pudieron generar los problemas en este momento. Inténtalo de nuevo.'
const LENGUAJES = CAMPOS_CURSO.find((campo) => campo.nombre === 'lenguaje').opciones

// Mismos nombres que espera admin-api. Cantidad: entre 1 y 10 (MAX_CANTIDAD en problemas/serializers.py).
const CAMPOS = [
  { nombre: 'tema', etiqueta: 'Tema', tipo: 'text' },
  { nombre: 'nivel', etiqueta: 'Nivel', tipo: 'select', opciones: DIFICULTADES },
  { nombre: 'lenguaje', etiqueta: 'Lenguaje', tipo: 'select', opciones: LENGUAJES },
  { nombre: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', min: 1, max: 10 },
]

// admin-api responde los errores de validación como { campo: ['mensaje', ...] }.
function erroresDelServidor(cuerpo) {
  const errores = {}
  for (const campo of CAMPOS) {
    if (Array.isArray(cuerpo[campo.nombre])) {
      errores[campo.nombre] = cuerpo[campo.nombre].join(' ')
    }
  }
  if (cuerpo.detail) {
    errores.general = cuerpo.detail
  }
  if (Object.keys(errores).length === 0) {
    errores.general = ERROR_GENERAR
  }
  return errores
}

function GenerarProblemas({ id }) {
  // null mientras carga.
  const [curso, setCurso] = useState(null)
  const [errorCarga, setErrorCarga] = useState(false)
  const [noExiste, setNoExiste] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})
  // Problemas creados; mientras sea null se muestra el formulario.
  const [creados, setCreados] = useState(null)
  // Lo último que se envió: «Generar más» vuelve al formulario con esos valores.
  const [ultimos, setUltimos] = useState(null)
  // Cada envío gasta cuota del proveedor de IA. El estado tarda un render en deshabilitar el botón;
  // la marca bloquea al instante un segundo envío.
  const generando = useRef(false)

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

  async function generar(valores) {
    if (generando.current) {
      return
    }
    generando.current = true
    setEnviando(true)
    setErrores({})
    setUltimos(valores)
    try {
      const { ok, status, cuerpo } = await generarProblemas(id, { ...valores, cantidad: Number(valores.cantidad) })
      if (ok) {
        setCreados(cuerpo.problemas ?? [])
      } else if (status === 404) {
        setNoExiste(true)
      } else if (status === 403) {
        setErrores({ general: 'No tienes permiso para generar problemas en este curso.' })
      } else if (status === 400) {
        setErrores(erroresDelServidor(cuerpo))
      } else {
        // 5xx: incluye el 503 (IA caída) y el 502 (respuesta inservible) con su mensaje.
        setErrores({ general: cuerpo.detail || ERROR_GENERAR })
      }
    } catch (error) {
      // fetch lanza error sin conexión o cuando se agota el tiempo máximo.
      setErrores({
        general: error.name === 'TimeoutError' ? 'La generación tardó demasiado. Inténtalo de nuevo.' : ERROR_GENERAR,
      })
    } finally {
      generando.current = false
      setEnviando(false)
    }
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
  } else if (creados !== null) {
    contenido = (
      <>
        <Ilustracion nombre="exito" className="generacion-exito" />
        <p>
          {creados.length === 1 ? 'Se generó 1 problema.' : `Se generaron ${creados.length} problemas.`} Quedaron
          pendientes de revisión: ningún estudiante los verá hasta que los apruebes.
        </p>
        {creados.length > 0 && (
          <ul>
            {creados.map((problema) => (
              <li key={problema.id}>{problema.titulo}</li>
            ))}
          </ul>
        )}
        <p>
          <button type="button" onClick={() => setCreados(null)}>
            Generar más
          </button>{' '}
          <a href="/revision">Ir a la bandeja de revisión</a> <a href="/cursos">Volver a Mis cursos</a>
        </p>
      </>
    )
  } else {
    const iniciales = ultimos ?? {
      // El lenguaje del curso preseleccionado, si es una de las opciones.
      lenguaje: LENGUAJES.some((opcion) => opcion.valor === curso.lenguaje) ? curso.lenguaje : '',
      cantidad: '3',
    }
    // Formulario toma los valores iniciales solo al montarse: tras un error conserva lo escrito.
    contenido = (
      <>
        <p>La IA crea borradores. Tú los revisas antes de que lleguen a tus estudiantes.</p>
        <Formulario
          campos={CAMPOS}
          textoBoton={enviando ? 'Generando…' : 'Generar problemas'}
          enviando={enviando}
          errores={errores}
          onEnviar={generar}
          valoresIniciales={iniciales}
        />
        {enviando && (
          <div className="generacion-progreso" role="status" aria-busy="true">
            <p>Generando problemas… esto puede tardar hasta un minuto. No cierres esta página.</p>
            {/* Indeterminada: no hay porcentaje real que mostrar. */}
            <div className="generacion-progreso-barra" aria-hidden="true" />
          </div>
        )}
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
        <h1>Generar problemas con IA</h1>
        {curso && <p>{curso.nombre}</p>}
      </Hero>
      {curso && creados === null ? <div className="panel-formulario">{contenido}</div> : contenido}
    </main>
  )
}

export default GenerarProblemas
