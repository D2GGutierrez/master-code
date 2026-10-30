import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import { misCursos, type MiCurso } from '../api/cursos.ts'
import { listarProblemas, type ProblemaDeLista } from '../api/problemas.ts'
import Icono from '../components/Icono.tsx'
import { EstadoVacio, Hero } from '../components/Pantalla.tsx'
import { AnilloProgreso } from '../components/Progreso.tsx'
import { etiquetaDificultad, etiquetaEstado, etiquetaTipoEjercicio } from '../etiquetas.ts'
import { claveLenguaje, etiquetaLenguaje } from '../lenguajes.ts'
import { resumir, type Resumen } from '../progreso.ts'
import './ProblemasCurso.css'

// --- HU30-T3: filtros que aplica user-api y el último filtro recordado. ---
// Valores exactos de user-api; de aquí salen los botones y la validación de lo guardado.
const DIFICULTADES = ['facil', 'medio', 'dificil'] as const
const ESTADOS = ['resuelto', 'intentado', 'sin_abrir'] as const
// Una sola clave para todos los cursos: el filtro se recuerda, no se guarda por curso.
const CLAVE_FILTRO = 'mc_filtro_problemas'

// '' = «todos» (sin parámetro).
type Filtro = { dificultad: (typeof DIFICULTADES)[number] | ''; estado: (typeof ESTADOS)[number] | '' }
const SIN_FILTRO: Filtro = { dificultad: '', estado: '' }

// Lo guardado puede estar corrupto o venir de otra versión: cada campo se valida y lo inválido es «todos».
function leerFiltro(): Filtro {
  try {
    const guardado: unknown = JSON.parse(localStorage.getItem(CLAVE_FILTRO) ?? 'null')
    if (typeof guardado !== 'object' || guardado === null || Array.isArray(guardado)) {
      return SIN_FILTRO
    }
    const { dificultad, estado } = guardado as Record<string, unknown>
    return {
      dificultad: DIFICULTADES.find((valor) => valor === dificultad) ?? '',
      estado: ESTADOS.find((valor) => valor === estado) ?? '',
    }
  } catch {
    // JSON roto o almacenamiento bloqueado (p. ej. navegación privada): sin filtro.
    return SIN_FILTRO
  }
}

function guardarFiltro(filtro: Filtro) {
  try {
    localStorage.setItem(CLAVE_FILTRO, JSON.stringify(filtro))
  } catch {
    // Sin almacenamiento la pantalla funciona igual; solo no recuerda el filtro.
  }
}
// --- fin de los filtros ---

// --- HU29-T3: orden y paginación en el navegador, sobre la lista que ya llega (user-api no ordena ni pagina). ---
const PROBLEMAS_POR_PAGINA = 10

// Rango y no orden alfabético («dificil» iría antes que «facil»). Mismos valores que admin-api (DIFICULTADES).
const RANGO_DIFICULTAD = new Map([
  ['facil', 1],
  ['medio', 2],
  ['dificil', 3],
])

type Orden = 'dificultad-asc' | 'dificultad-desc' | 'recientes' | 'antiguos'

function rangoDe(dificultad: string) {
  return RANGO_DIFICULTAD.get(dificultad.trim().toLowerCase()) ?? null
}

// null si no hay fecha legible: esos problemas van al final.
function tiempoDe(fecha: string) {
  const tiempo = Date.parse(fecha)
  return Number.isNaN(tiempo) ? null : tiempo
}

// Compara dos valores que pueden faltar: el que falta va al final, en cualquier dirección del orden.
function conFaltantesAlFinal(a: number | null, b: number | null, ascendente: boolean) {
  if (a === null || b === null) {
    return (a === null ? 1 : 0) - (b === null ? 1 : 0)
  }
  return ascendente ? a - b : b - a
}

function compararProblemas(orden: Orden) {
  return (a: ProblemaDeLista, b: ProblemaDeLista) => {
    const porTitulo = a.titulo.localeCompare(b.titulo, 'es')
    if (orden === 'recientes' || orden === 'antiguos') {
      return (
        conFaltantesAlFinal(tiempoDe(a.fecha_publicacion), tiempoDe(b.fecha_publicacion), orden === 'antiguos') ||
        porTitulo
      )
    }
    // Desempate: el más reciente primero y luego el título.
    return (
      conFaltantesAlFinal(rangoDe(a.dificultad), rangoDe(b.dificultad), orden === 'dificultad-asc') ||
      conFaltantesAlFinal(tiempoDe(a.fecha_publicacion), tiempoDe(b.fecha_publicacion), false) ||
      porTitulo
    )
  }
}

// La página pedida se limita al rango válido: así nunca queda una página vacía si cambia el total.
function paginacion(total: number, pagina: number) {
  const totalPaginas = Math.max(1, Math.ceil(total / PROBLEMAS_POR_PAGINA))
  const actual = Math.min(Math.max(1, pagina), totalPaginas)
  const inicio = (actual - 1) * PROBLEMAS_POR_PAGINA
  return {
    totalPaginas,
    actual,
    inicio,
    fin: Math.min(inicio + PROBLEMAS_POR_PAGINA, total),
    conControles: total > PROBLEMAS_POR_PAGINA,
  }
}
// --- fin de la lógica de orden y paginación ---

// Mensaje según el código de user-api. Un curso que no existe también da 403: nadie está inscrito en él.
function mensajeDeError(status: number) {
  if (status === 403) {
    return 'No estás inscrito en este curso.'
  }
  if (status === 404) {
    return 'El curso no existe o ya no está disponible.'
  }
  return ''
}

function ProblemasCurso({ id }: { id: number }) {
  const [problemas, setProblemas] = useState<ProblemaDeLista[] | null>(null)
  // null mientras carga; si no está entre «Mis cursos», no hay datos del curso y el título es «Curso».
  const [datosCurso, setDatosCurso] = useState<MiCurso | null>(null)
  const [cursoListo, setCursoListo] = useState(false)
  // Progreso del curso entero (sin filtros): undefined mientras llega, null si no se pudo cargar.
  const [resumen, setResumen] = useState<Resumen | null | undefined>(undefined)
  // 403 y 404 tienen su mensaje y no se reintentan; cualquier otro fallo permite «Reintentar».
  const [sinAcceso, setSinAcceso] = useState('')
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga de problemas.
  const [recargas, setRecargas] = useState(0)
  const [orden, setOrden] = useState<Orden>('dificultad-asc')
  const [pagina, setPagina] = useState(1)
  // Se lee una sola vez, al montar.
  const [filtro, setFiltro] = useState(leerFiltro)
  const hayFiltros = filtro.dificultad !== '' || filtro.estado !== ''
  const lista = useRef<HTMLUListElement>(null)

  // Una copia: el arreglo que llegó del servidor no se modifica.
  const ordenados = useMemo(() => (problemas ? [...problemas].sort(compararProblemas(orden)) : []), [problemas, orden])
  const hayFechas = ordenados.some((problema) => tiempoDe(problema.fecha_publicacion) !== null)
  const { totalPaginas, actual, inicio, fin, conControles } = paginacion(ordenados.length, pagina)

  function cambiarOrden(valor: Orden) {
    setOrden(valor)
    setPagina(1)
  }

  // El orden elegido se conserva; la página vuelve a 1 porque la lista cambia.
  function cambiarFiltro(nuevo: Filtro) {
    if (nuevo.dificultad === filtro.dificultad && nuevo.estado === filtro.estado) {
      return
    }
    setFiltro(nuevo)
    guardarFiltro(nuevo)
    setProblemas(null)
    setPagina(1)
  }

  function cambiarPagina(nueva: number) {
    setPagina(nueva)
    lista.current?.scrollIntoView({ block: 'start' })
  }

  function reintentar() {
    setError(false)
    setRecargas(recargas + 1)
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    // Si el filtro cambia antes de la respuesta, esta petición se cancela: una respuesta vieja nunca pisa a una nueva.
    const peticion = new AbortController()
    listarProblemas(id, filtro, peticion.signal).then(
      (respuesta) => {
        if (peticion.signal.aborted) {
          return
        }
        if (respuesta.ok) {
          setProblemas(respuesta.problemas)
        } else if (mensajeDeError(respuesta.status)) {
          setSinAcceso(mensajeDeError(respuesta.status))
        } else {
          setError(true)
        }
      },
      () => {
        if (!peticion.signal.aborted) {
          setError(true)
        }
      },
    )
    return () => peticion.abort()
  }, [id, recargas, filtro])

  useEffect(() => {
    // La lista de problemas no trae el nombre ni el lenguaje del curso: salen de «Mis cursos». Si falla, el título es genérico.
    misCursos().then(
      (cursos) => {
        setDatosCurso(cursos.find((curso) => curso.curso_id === id) ?? null)
        setCursoListo(true)
      },
      () => setCursoListo(true),
    )
  }, [id])

  useEffect(() => {
    // El anillo del hero cuenta el curso entero: se pide aparte, sin los filtros de la lista.
    let activo = true
    listarProblemas(id).then(
      (respuesta) => {
        if (activo) {
          setResumen(respuesta.ok ? resumir(respuesta.problemas) : null)
        }
      },
      () => {
        if (activo) {
          setResumen(null)
        }
      },
    )
    return () => {
      activo = false
    }
  }, [id, recargas])

  // Se muestran mientras carga y con un resultado vacío, para poder cambiar o quitar el filtro.
  const botonesFiltro = (
    <div className="filtros-problemas">
      <div role="group" aria-labelledby="filtro-dificultad" className="grupo-filtro">
        <span id="filtro-dificultad">Dificultad</span>
        {(['', ...DIFICULTADES] as const).map((valor) => (
          <button
            key={valor || 'todas'}
            type="button"
            className="boton-filtro"
            aria-pressed={filtro.dificultad === valor}
            onClick={() => cambiarFiltro({ ...filtro, dificultad: valor })}
          >
            {valor ? etiquetaDificultad(valor) : 'Todas'}
          </button>
        ))}
      </div>
      <div role="group" aria-labelledby="filtro-estado" className="grupo-filtro">
        <span id="filtro-estado">Estado</span>
        {(['', ...ESTADOS] as const).map((valor) => (
          <button
            key={valor || 'todos'}
            type="button"
            className="boton-filtro"
            aria-pressed={filtro.estado === valor}
            onClick={() => cambiarFiltro({ ...filtro, estado: valor })}
          >
            {valor ? etiquetaEstado(valor) : 'Todos'}
          </button>
        ))}
      </div>
    </div>
  )

  const nombreCurso = cursoListo ? (datosCurso?.nombre ?? 'Curso') : 'Curso'
  const clave = claveLenguaje(datosCurso?.lenguaje ?? '')
  const anillo =
    resumen === undefined ? (
      <div className="hero-anillo anima" style={{ '--i': 2 } as CSSProperties} role="status">
        <span className="solo-lectores">Cargando tu progreso…</span>
        <div className="esqueleto esqueleto-anillo" aria-hidden="true" />
      </div>
    ) : resumen && resumen.total > 0 ? (
      <div className="hero-anillo anima" style={{ '--i': 2 } as CSSProperties}>
        <AnilloProgreso
          valor={resumen.fraccion}
          tamano="9rem"
          etiqueta={`Progreso del curso: ${resumen.resueltos} de ${resumen.total} problemas resueltos`}
        />
        <p>Progreso del curso</p>
      </div>
    ) : undefined

  return (
    <main className="problemas-curso">
      <Hero clase={`hero-curso leng-${clave}${anillo ? ' hero-con-anillo' : ''}`} lateral={anillo}>
        <p className="hero-sobretitulo">Problemas del curso</p>
        <h1>{nombreCurso}</h1>
        {datosCurso && (
          <p className="hero-meta">
            <span className="hero-etiqueta">{etiquetaLenguaje(datosCurso.lenguaje)}</span>
            <span className="hero-etiqueta">{datosCurso.institucion}</span>
          </p>
        )}
        {resumen && resumen.total > 0 && (
          <ul className="hero-cifras" aria-label="Tu progreso en el curso">
            <li>
              <strong>{resumen.resueltos}</strong>
              <span>{resumen.resueltos === 1 ? 'resuelto' : 'resueltos'}</span>
            </li>
            <li>
              <strong>{resumen.intentados}</strong>
              <span>{resumen.intentados === 1 ? 'intentado' : 'intentados'}</span>
            </li>
            <li>
              <strong>{resumen.sinAbrir}</strong>
              <span>sin abrir</span>
            </li>
          </ul>
        )}
        <p className="hero-nota">
          <a href="/inicio">Volver a Mis cursos</a>
        </p>
      </Hero>
      {sinAcceso ? (
        <p role="alert">{sinAcceso}</p>
      ) : error ? (
        <>
          <p role="alert">No se pudieron cargar los problemas. Inténtalo de nuevo.</p>
          <button type="button" onClick={reintentar}>Reintentar</button>
        </>
      ) : !problemas ? (
        <>
          {botonesFiltro}
          <p role="status">Cargando problemas…</p>
        </>
      ) : problemas.length === 0 && hayFiltros ? (
        <>
          {botonesFiltro}
          <p aria-live="polite">
            Ningún problema coincide con los filtros.{' '}
            <button type="button" onClick={() => cambiarFiltro(SIN_FILTRO)}>
              Quitar filtros
            </button>
          </p>
        </>
      ) : problemas.length === 0 ? (
        <EstadoVacio>
          <p>Este curso todavía no tiene problemas publicados.</p>
        </EstadoVacio>
      ) : (
        <>
          {botonesFiltro}
          <div className="controles-problemas">
            <label htmlFor="orden-problemas">Ordenar por</label>
            <select id="orden-problemas" value={orden} onChange={(evento) => cambiarOrden(evento.target.value as Orden)}>
              <option value="dificultad-asc">Dificultad: de fácil a difícil</option>
              <option value="dificultad-desc">Dificultad: de difícil a fácil</option>
              {hayFechas && <option value="recientes">Más recientes primero</option>}
              {hayFechas && <option value="antiguos">Más antiguos primero</option>}
            </select>
          </div>
          <p aria-live="polite">
            {ordenados.length === 1 ? '1 problema' : `Mostrando ${inicio + 1}–${fin} de ${ordenados.length} problemas`}
          </p>
          <ul ref={lista} className="lista-problemas">
            {ordenados.slice(inicio, fin).map((problema, indice) => (
              <li key={problema.id} className="problema anima" style={{ '--i': indice } as CSSProperties}>
                <span className={`estado-icono estado-${problema.estado}`} aria-hidden="true">
                  {problema.estado === 'resuelto' && <Icono nombre="correcto" />}
                </span>
                <div className="problema-cuerpo">
                  <a className="problema-titulo" href={`/problemas/${problema.id}`}>
                    {problema.titulo}
                  </a>
                  <div className="problema-etiquetas">
                    <span className={`etiqueta-problema dificultad dificultad-${problema.dificultad}`}>
                      {etiquetaDificultad(problema.dificultad)}
                    </span>
                    <span className="etiqueta-problema">{etiquetaEstado(problema.estado)}</span>
                    <span className="etiqueta-problema">{etiquetaTipoEjercicio(problema.tipo_ejercicio)}</span>
                  </div>
                </div>
                <Icono nombre="flecha" />
              </li>
            ))}
          </ul>
          {conControles && (
            <nav className="controles-problemas" aria-label="Páginas de problemas">
              <button type="button" disabled={actual === 1} onClick={() => cambiarPagina(actual - 1)}>
                Anterior
              </button>
              <span>
                Página {actual} de {totalPaginas}
              </span>
              <button type="button" disabled={actual === totalPaginas} onClick={() => cambiarPagina(actual + 1)}>
                Siguiente
              </button>
            </nav>
          )}
        </>
      )}
    </main>
  )
}

export default ProblemasCurso
