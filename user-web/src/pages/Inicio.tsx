import { useEffect, useState, type CSSProperties } from 'react'
import { misCursos, type MiCurso } from '../api/cursos.ts'
import { correoDeSesion } from '../api/sesion.ts'
import { EsqueletoCursos } from '../components/Esqueleto.tsx'
import { FiltroLenguaje } from '../components/FiltroLenguaje.tsx'
import Icono from '../components/Icono.tsx'
import { EstadoVacio, Hero } from '../components/Pantalla.tsx'
import { Portada } from '../components/Portada.tsx'
import { BarraProgreso } from '../components/Progreso.tsx'
import { TarjetaCurso } from '../components/TarjetaCurso.tsx'
import { claveLenguaje, etiquetaLenguaje, type ClaveLenguaje } from '../lenguajes.ts'
import { nombreDesdeCorreo } from '../persona.ts'
import { elegirContinuar, resumir, resumirTodos } from '../progreso.ts'
import { useProgreso, type DatosProgreso } from '../useProgreso.ts'
import './Inicio.css'

function plural(cantidad: number, singular: string, varios: string) {
  return cantidad === 1 ? singular : varios
}

// La tarjeta de cristal del hero: el curso donde se quedó el estudiante y el siguiente problema (datos reales:
// el último envío y el estado de cada problema). Mientras llegan, un marcador con brillo.
// Marcador con brillo del panel mientras llegan los cursos o su progreso: el hero tiene su forma desde el primer momento.
function PanelEsqueleto() {
  return (
    <aside className="hero-panel anima" style={{ '--i': 2 } as CSSProperties} role="status">
      <span className="solo-lectores">Buscando tu último curso…</span>
      <div aria-hidden="true">
        <div className="esqueleto" style={{ height: '0.75rem', width: '50%' }} />
        <div className="esqueleto" style={{ height: '3.5rem', marginTop: '1rem' }} />
        <div className="esqueleto" style={{ height: '0.5rem', marginTop: '1rem' }} />
      </div>
    </aside>
  )
}

// La tarjeta de cristal del hero: el curso donde se quedó el estudiante y el siguiente problema (datos reales:
// el último envío y el estado de cada problema).
function PanelContinuar({ cursos, progreso }: { cursos: MiCurso[]; progreso: DatosProgreso }) {
  const entrada = { '--i': 2 } as CSSProperties
  if (!progreso.listo) {
    return <PanelEsqueleto />
  }
  const continuar = elegirContinuar(cursos, progreso.problemas, progreso.avance)
  if (!continuar) {
    return null
  }
  const { curso, problema, conActividad } = continuar
  const lista = progreso.problemas.get(curso.curso_id)
  const resumen = lista ? resumir(lista) : null
  const verCurso = `/cursos/${curso.curso_id}/problemas`
  return (
    <aside className="hero-panel anima" style={entrada} aria-labelledby="panel-continuar">
      <p className="hero-sobretitulo" id="panel-continuar">
        {conActividad ? 'Continúa donde lo dejaste' : 'Empieza por aquí'}
      </p>
      <div className="panel-curso">
        <Portada lenguaje={curso.lenguaje} variante={curso.curso_id} />
        <div>
          <h2>{curso.nombre}</h2>
          <p>
            {etiquetaLenguaje(curso.lenguaje)} · {curso.institucion}
          </p>
        </div>
      </div>
      {resumen && resumen.total > 0 && (
        <BarraProgreso
          valor={resumen.fraccion}
          etiqueta={`Progreso en ${curso.nombre}: ${resumen.resueltos} de ${resumen.total} problemas resueltos`}
        >
          <span>
            <strong>{resumen.resueltos}</strong> de {resumen.total} resueltos
          </span>
          <strong>{Math.round(resumen.fraccion * 100)} %</strong>
        </BarraProgreso>
      )}
      {problema ? (
        <p className="panel-siguiente">
          <span>Siguiente problema</span>
          <strong>{problema.titulo}</strong>
        </p>
      ) : (
        resumen &&
        resumen.total > 0 && (
          <p className="panel-siguiente">
            <strong>¡Completaste este curso!</strong>
          </p>
        )
      )}
      <div className="hero-acciones">
        {problema ? (
          <>
            <a className="boton-hero" href={`/problemas/${problema.id}`}>
              {conActividad ? 'Continuar' : 'Empezar'}
              <Icono nombre="flecha" />
            </a>
            <a className="boton-hero-fantasma" href={verCurso}>
              Ver curso
            </a>
          </>
        ) : (
          <a className="boton-hero" href={verCurso}>
            Ver curso
            <Icono nombre="flecha" />
          </a>
        )}
      </div>
    </aside>
  )
}

function Inicio() {
  const [cursos, setCursos] = useState<MiCurso[] | null>(null)
  const [error, setError] = useState(false)
  // '' = todos los lenguajes.
  const [filtro, setFiltro] = useState<ClaveLenguaje | ''>('')
  const progreso = useProgreso(cursos)
  const nombre = nombreDesdeCorreo(correoDeSesion())

  function cargar() {
    misCursos().then(setCursos, () => setError(true))
  }

  function reintentar() {
    setError(false)
    cargar()
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    cargar()
  }, [])

  const hayCursos = cursos !== null && cursos.length > 0
  const total = resumirTodos(progreso.problemas)
  // Un filtro sin cursos (no debería pasar) no deja la pantalla vacía.
  const visibles = (cursos ?? []).filter((curso) => !filtro || claveLenguaje(curso.lenguaje) === filtro)
  const mostrados = visibles.length > 0 ? visibles : (cursos ?? [])

  return (
    <main className="inicio">
      <Hero
        lateral={
          cursos === null && !error ? (
            <PanelEsqueleto />
          ) : hayCursos ? (
            <PanelContinuar cursos={cursos} progreso={progreso} />
          ) : undefined
        }
      >
        <p className="hero-sobretitulo">Mis cursos</p>
        <h1>
          {nombre ? (
            <>
              ¡Hola, <span className="hero-nombre">{nombre}</span>!
            </>
          ) : (
            '¡Hola de nuevo!'
          )}
        </h1>
        <p>Entra a un curso para resolver sus problemas y ver cómo avanzas.</p>
        <div className="hero-acciones">
          <a className="boton-hero" href="/cursos/inscribirse">
            Inscribirme con un código
          </a>
          <a className="boton-hero-fantasma" href="/cursos/buscar">
            Buscar cursos
          </a>
        </div>
        {hayCursos && (
          <ul className="hero-cifras" aria-label="Tu resumen">
            <li>
              <strong>{cursos.length}</strong>
              <span>{plural(cursos.length, 'curso', 'cursos')}</span>
            </li>
            <li>
              <strong>{progreso.listo ? total.resueltos : '–'}</strong>
              <span>{plural(total.resueltos, 'problema resuelto', 'problemas resueltos')}</span>
            </li>
            <li>
              <strong>{progreso.listo ? total.intentados : '–'}</strong>
              <span>{plural(total.intentados, 'problema en progreso', 'problemas en progreso')}</span>
            </li>
          </ul>
        )}
      </Hero>
      <section aria-labelledby="tus-cursos">
        <div className="seccion-cabecera">
          <h2 id="tus-cursos">
            Tus cursos {cursos && cursos.length > 0 && <span className="contador">{cursos.length}</span>}
          </h2>
          {hayCursos && <FiltroLenguaje cursos={cursos} valor={filtro} onCambio={setFiltro} />}
        </div>
        {error ? (
          <>
            <p role="alert">No se pudieron cargar tus cursos. Inténtalo de nuevo.</p>
            <button type="button" onClick={reintentar}>Reintentar</button>
          </>
        ) : !cursos ? (
          <EsqueletoCursos aviso="Cargando tus cursos…" />
        ) : cursos.length === 0 ? (
          <EstadoVacio>
            <p>Todavía no estás inscrito en ningún curso.</p>
          </EstadoVacio>
        ) : (
          <ul className="rejilla-cursos">
            {mostrados.map((curso, indice) => {
              const lista = progreso.problemas.get(curso.curso_id)
              return (
                <TarjetaCurso
                  key={curso.curso_id}
                  curso={curso}
                  indice={indice}
                  enlace={`/cursos/${curso.curso_id}/problemas`}
                  detalle={`Inscrito el ${new Date(curso.fecha_inscripcion).toLocaleDateString('es-PE')}`}
                  progreso={lista ? resumir(lista) : lista === null || progreso.listo ? null : 'cargando'}
                />
              )
            })}
          </ul>
        )}
      </section>
    </main>
  )
}

export default Inicio
