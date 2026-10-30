import { useEffect, useState } from 'react'
import { obtenerProblema, type ProblemaDetalle } from '../api/problemas.ts'
import EjecutorCodigo from '../components/EjecutorCodigo.tsx'
import EnvioSolucion from '../components/EnvioSolucion.tsx'
import Icono from '../components/Icono.tsx'
import { Hero } from '../components/Pantalla.tsx'
import { claveLenguaje, etiquetaLenguaje } from '../lenguajes.ts'
import './EditorProblema.css'

// --- HU32-T3: cuándo un problema usa el editor. ---
// Solo la escritura de código usa el editor y el sandbox; los otros tipos se responden en el celular.
const TIPO_CON_EDITOR = 'escritura_codigo'

function correspondeEditor(problema: Pick<ProblemaDetalle, 'tipo_ejercicio' | 'plataforma'>) {
  return problema.tipo_ejercicio === TIPO_CON_EDITOR && problema.plataforma !== 'movil'
}
// --- fin de la condición ---

// Mensaje según el código de user-api. Hoy responde 404 también si no está inscrito, para no revelar qué existe.
function mensajeDeError(status: number) {
  if (status === 403) {
    return 'No estás inscrito en el curso de este problema.'
  }
  if (status === 404) {
    return 'El problema no existe o no está disponible.'
  }
  return ''
}

// Pantalla mínima: HU33 la reemplazará por los paneles con el enunciado junto al editor. Sin código base (HU34) ni
// borrador (HU35). «Enviar solución» es HU36.
function EditorProblema({ id }: { id: number }) {
  const [problema, setProblema] = useState<ProblemaDetalle | null>(null)
  // 403 y 404 tienen su mensaje y no se reintentan; cualquier otro fallo permite «Reintentar».
  const [sinAcceso, setSinAcceso] = useState('')
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)
  const [codigo, setCodigo] = useState('')

  function reintentar() {
    setError(false)
    setRecargas(recargas + 1)
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    obtenerProblema(id).then(
      (respuesta) => {
        if (respuesta.ok) {
          setProblema(respuesta.problema)
        } else if (mensajeDeError(respuesta.status)) {
          setSinAcceso(mensajeDeError(respuesta.status))
        } else {
          setError(true)
        }
      },
      () => setError(true),
    )
  }, [id, recargas])

  const volver = <a href={`/problemas/${id}`}>Volver al problema</a>

  if (sinAcceso || error || !problema) {
    return (
      <main className="editor-problema">
        <p>{volver}</p>
        {sinAcceso ? (
          <p role="alert">{sinAcceso}</p>
        ) : error ? (
          <>
            <p role="alert">No se pudo cargar el problema. Inténtalo de nuevo.</p>
            <button type="button" onClick={reintentar}>Reintentar</button>
          </>
        ) : (
          <p>Cargando problema…</p>
        )}
      </main>
    )
  }

  // Entrar por URL a un problema que no es de código no monta el editor.
  if (!correspondeEditor(problema)) {
    return (
      <main className="editor-problema">
        <p>{volver}</p>
        <p>
          {problema.plataforma === 'movil'
            ? 'Este problema es solo para la app móvil.'
            : 'Este problema no usa el editor de código.'}
        </p>
      </main>
    )
  }

  return (
    <main className="editor-problema">
      <Hero clase={`hero-compacto hero-curso leng-${claveLenguaje(problema.lenguaje)}`}>
        <p className="hero-volver">
          <a href={`/problemas/${id}`}>
            <Icono nombre="atras" />
            Volver al problema
          </a>
        </p>
        <h1>{problema.titulo}</h1>
        <p className="hero-meta">
          <span className="hero-etiqueta etiqueta-lenguaje">{etiquetaLenguaje(problema.lenguaje)}</span>
        </p>
      </Hero>
      {/* Igual que /editor-prueba: EjecutorCodigo ya incluye el editor y traduce el lenguaje con idMonaco. */}
      <EjecutorCodigo valor={codigo} onCambio={setCodigo} lenguaje={problema.lenguaje} />
      <EnvioSolucion problemaId={id} codigo={codigo} />
    </main>
  )
}

export default EditorProblema
