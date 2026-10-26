import { useEffect, useRef, useState, type FormEvent } from 'react'
import { buscarCursos, type CursoDisponible } from '../api/cursos.ts'
import { etiquetaLenguaje } from '../lenguajes.ts'
import './BuscarCursos.css'

// HU25-T3: solo consulta. Las tarjetas no son enlaces: inscribirse pide el código de invitación del docente.
function BuscarCursos() {
  const [cursos, setCursos] = useState<CursoDisponible[] | null>(null)
  const [error, setError] = useState(false)
  // Empieza cargando: la primera petición sale al montar la pantalla.
  const [cargando, setCargando] = useState(true)
  // texto: lo que hay en el campo; busqueda: el término de la última petición (también la que se reintenta).
  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState('')
  // El estado tarda un render en deshabilitar el botón; la marca bloquea al instante una segunda petición.
  const enCurso = useRef(true)

  async function cargar(termino: string) {
    if (enCurso.current) {
      return
    }
    enCurso.current = true
    setCargando(true)
    setError(false)
    setBusqueda(termino)
    try {
      setCursos(await buscarCursos(termino))
    } catch {
      setError(true)
    } finally {
      enCurso.current = false
      setCargando(false)
    }
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    // Sin cambiar el estado antes de la respuesta: ya empieza cargando y con la marca puesta.
    buscarCursos('')
      .then(setCursos, () => setError(true))
      .finally(() => {
        enCurso.current = false
        setCargando(false)
      })
  }, [])

  // Búsqueda al enviar (botón o Enter), no en vivo: una petición por búsqueda, no por tecla.
  function buscar(evento: FormEvent) {
    evento.preventDefault()
    cargar(texto.trim())
  }

  function quitarBusqueda() {
    setTexto('')
    cargar('')
  }

  return (
    <main className="buscar-cursos">
      <h1>Buscar cursos</h1>
      <p>
        <a href="/inicio">Volver a Mis cursos</a>
      </p>
      <form role="search" onSubmit={buscar}>
        <label htmlFor="buscar-cursos">Buscar cursos por nombre</label>{' '}
        <input id="buscar-cursos" type="search" value={texto} onChange={(evento) => setTexto(evento.target.value)} />{' '}
        <button type="submit" disabled={cargando}>
          Buscar
        </button>
      </form>
      <p>Pídele a tu docente el código de invitación para inscribirte.</p>
      {error ? (
        <>
          <p role="alert">No se pudieron cargar los cursos. Inténtalo de nuevo.</p>
          <button type="button" onClick={() => cargar(busqueda)}>
            Reintentar
          </button>
        </>
      ) : cargando || !cursos ? (
        <p>Cargando cursos…</p>
      ) : cursos.length === 0 && !busqueda ? (
        <p>Todavía no hay cursos publicados en tu institución.</p>
      ) : cursos.length === 0 ? (
        <p aria-live="polite">
          No se encontraron cursos para “{busqueda}”.{' '}
          <button type="button" onClick={quitarBusqueda}>
            Quitar búsqueda
          </button>
        </p>
      ) : (
        <>
          <p aria-live="polite">{cursos.length === 1 ? '1 curso' : `${cursos.length} cursos`}</p>
          <ul className="buscar-cursos-lista">
            {cursos.map((curso) => (
              <li key={curso.curso_id} className="curso-disponible">
                <h2>{curso.nombre}</h2>
                <p>{etiquetaLenguaje(curso.lenguaje)}</p>
                <p className="curso-disponible-institucion">{curso.institucion}</p>
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  )
}

export default BuscarCursos
