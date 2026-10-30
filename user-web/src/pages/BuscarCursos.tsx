import { useEffect, useRef, useState, type FormEvent } from 'react'
import { buscarCursos, type CursoDisponible } from '../api/cursos.ts'
import { EsqueletoCursos } from '../components/Esqueleto.tsx'
import { FiltroLenguaje } from '../components/FiltroLenguaje.tsx'
import Icono from '../components/Icono.tsx'
import { EstadoVacio, Hero } from '../components/Pantalla.tsx'
import { TarjetaCurso } from '../components/TarjetaCurso.tsx'
import { claveLenguaje, type ClaveLenguaje } from '../lenguajes.ts'
import './BuscarCursos.css'

// HU25-T3: solo consulta. Las tarjetas no son enlaces: inscribirse pide el código de invitación del docente
// y se hace en /cursos/inscribirse.
function BuscarCursos() {
  const [cursos, setCursos] = useState<CursoDisponible[] | null>(null)
  const [error, setError] = useState(false)
  // Empieza cargando: la primera petición sale al montar la pantalla.
  const [cargando, setCargando] = useState(true)
  // texto: lo que hay en el campo; busqueda: el término de la última petición (también la que se reintenta).
  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState('')
  // Filtro por lenguaje sobre lo que ya llegó ('' = todos).
  const [filtro, setFiltro] = useState<ClaveLenguaje | ''>('')
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
    setFiltro('')
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

  const visibles = (cursos ?? []).filter((curso) => !filtro || claveLenguaje(curso.lenguaje) === filtro)
  const mostrados = visibles.length > 0 ? visibles : (cursos ?? [])

  return (
    <main className="buscar-cursos">
      <Hero>
        <p className="hero-sobretitulo">Explora</p>
        <h1>Buscar cursos</h1>
        <p>Mira qué cursos hay en tu institución. Para entrar a uno necesitas el código de invitación de tu docente.</p>
        <form role="search" className="hero-busqueda" onSubmit={buscar}>
          <label htmlFor="buscar-cursos" className="solo-lectores">
            Buscar cursos por nombre
          </label>
          <Icono nombre="buscar" />
          <input
            id="buscar-cursos"
            type="search"
            value={texto}
            placeholder="Busca un curso por nombre"
            onChange={(evento) => setTexto(evento.target.value)}
          />
          <button type="submit" disabled={cargando}>
            Buscar
          </button>
        </form>
        <p className="hero-nota">
          Pídele a tu docente el código de invitación y <a href="/cursos/inscribirse">úsalo para inscribirte</a>.
        </p>
      </Hero>
      <section aria-labelledby="cursos-disponibles">
        <div className="seccion-cabecera">
          <h2 id="cursos-disponibles">
            {busqueda ? `Resultados para “${busqueda}”` : 'Cursos disponibles'}{' '}
            {cursos && cursos.length > 0 && <span className="contador">{cursos.length}</span>}
          </h2>
          {cursos && cursos.length > 0 && <FiltroLenguaje cursos={cursos} valor={filtro} onCambio={setFiltro} />}
        </div>
        {error ? (
          <>
            <p role="alert">No se pudieron cargar los cursos. Inténtalo de nuevo.</p>
            <button type="button" onClick={() => cargar(busqueda)}>
              Reintentar
            </button>
          </>
        ) : cargando || !cursos ? (
          <EsqueletoCursos aviso="Cargando cursos…" />
        ) : cursos.length === 0 && !busqueda ? (
          <EstadoVacio>
            <p>Todavía no hay cursos publicados en tu institución.</p>
          </EstadoVacio>
        ) : cursos.length === 0 ? (
          <EstadoVacio>
            <p aria-live="polite">
              No se encontraron cursos para “{busqueda}”.{' '}
              <button type="button" onClick={quitarBusqueda}>
                Quitar búsqueda
              </button>
            </p>
          </EstadoVacio>
        ) : (
          <>
            <p className="solo-lectores" aria-live="polite">
              {cursos.length === 1 ? '1 curso' : `${cursos.length} cursos`}
            </p>
            <ul className="rejilla-cursos">
              {mostrados.map((curso, indice) => (
                <TarjetaCurso
                  key={curso.curso_id}
                  curso={curso}
                  indice={indice}
                  pie={
                    <>
                      <a href="/cursos/inscribirse">Inscribirme con un código</a>
                      <Icono nombre="flecha" />
                    </>
                  }
                />
              ))}
            </ul>
          </>
        )}
      </section>
    </main>
  )
}

export default BuscarCursos
