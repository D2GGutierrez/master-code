import { useEffect, useState, type CSSProperties } from 'react'
import { obtenerAvance, type Avance as DatosAvance } from '../api/avance.ts'
import { misCursos, type MiCurso } from '../api/cursos.ts'
import type { ProblemaDeLista } from '../api/problemas.ts'
import Icono from '../components/Icono.tsx'
import { EstadoVacio, Hero } from '../components/Pantalla.tsx'
import { Portada } from '../components/Portada.tsx'
import { AnilloProgreso, BarraProgreso } from '../components/Progreso.tsx'
import { etiquetaEstado } from '../etiquetas.ts'
import { resumir, resumirTodos } from '../progreso.ts'
import { useProgreso } from '../useProgreso.ts'
import './Avance.css'

function plural(cantidad: number, singular: string, varios: string) {
  return cantidad === 1 ? singular : varios
}

function Avance() {
  const [avance, setAvance] = useState<DatosAvance | null>(null)
  const [error, setError] = useState(false)
  // Los cursos y sus problemas dan los títulos y los estados; si no llegan, la pantalla funciona con los números.
  const [cursos, setCursos] = useState<MiCurso[] | null>(null)
  const progreso = useProgreso(cursos, false)

  function cargar() {
    obtenerAvance().then(setAvance, () => setError(true))
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
    misCursos().then(setCursos, () => setCursos([]))
  }, [])

  const todos = resumirTodos(progreso.problemas)
  const hayTotales = progreso.listo && todos.total > 0
  // Para poner el título y el curso a cada fila de la tabla.
  const porProblema = new Map<number, { problema: ProblemaDeLista; curso: MiCurso }>()
  for (const curso of cursos ?? []) {
    for (const problema of progreso.problemas.get(curso.curso_id) ?? []) {
      porProblema.set(problema.id, { problema, curso })
    }
  }

  const anillo = hayTotales ? (
    <div className="hero-anillo anima" style={{ '--i': 2 } as CSSProperties}>
      <AnilloProgreso
        valor={todos.fraccion}
        tamano="9rem"
        etiqueta={`${todos.resueltos} de ${todos.total} problemas resueltos en todos tus cursos`}
      />
      <p>De todos tus cursos</p>
    </div>
  ) : undefined

  return (
    <main className="avance">
      <Hero clase="hero-con-anillo" lateral={avance && avance.total_envios > 0 ? anillo : undefined}>
        <p className="hero-sobretitulo">Tu progreso</p>
        <h1>Mi avance</h1>
        <p>Lo que has enviado desde la web y desde el móvil, en un solo lugar.</p>
      </Hero>
      {error ? (
        <>
          <p role="alert">No se pudo cargar tu avance. Inténtalo de nuevo.</p>
          <button type="button" onClick={reintentar}>Reintentar</button>
        </>
      ) : !avance ? (
        <p role="status">Cargando tu avance…</p>
      ) : avance.total_envios === 0 ? (
        <EstadoVacio>
          <p>Aún no has resuelto ejercicios. Cuando lo hagas, desde la web o desde el móvil, tu avance aparecerá aquí.</p>
        </EstadoVacio>
      ) : (
        <>
          <ul className="kpis">
            {hayTotales && (
              <li className="kpi anima" style={{ '--i': 0 } as CSSProperties}>
                <span className="kpi-icono" aria-hidden="true">
                  <Icono nombre="premio" />
                </span>
                <div>
                  <strong>{todos.resueltos}</strong>
                  <span>{plural(todos.resueltos, 'problema resuelto', 'problemas resueltos')}</span>
                </div>
              </li>
            )}
            <li className="kpi anima" style={{ '--i': 1 } as CSSProperties}>
              <span className="kpi-icono" aria-hidden="true">
                <Icono nombre="codigo" />
              </span>
              <div>
                {/* user-api cuenta los problemas con algún envío, resueltos o no. */}
                <strong>{avance.problemas_intentados}</strong>
                <span>{plural(avance.problemas_intentados, 'problema intentado', 'problemas intentados')}</span>
              </div>
            </li>
            <li className="kpi anima" style={{ '--i': 2 } as CSSProperties}>
              <span className="kpi-icono" aria-hidden="true">
                <Icono nombre="rayo" />
              </span>
              <div>
                <strong>{avance.total_envios}</strong>
                <span>
                  {plural(avance.total_envios, 'envío', 'envíos')} (web: {avance.desde_web}, móvil: {avance.desde_movil})
                </span>
              </div>
            </li>
          </ul>

          {cursos && cursos.length > 0 && progreso.listo && todos.total > 0 && (
            <section aria-labelledby="avance-cursos">
              <div className="seccion-cabecera">
                <h2 id="avance-cursos">Por curso</h2>
              </div>
              <ul className="curso-filas">
                {cursos.map((curso, indice) => {
                  const lista = progreso.problemas.get(curso.curso_id)
                  if (!lista || lista.length === 0) {
                    return null
                  }
                  const resumen = resumir(lista)
                  return (
                    <li key={curso.curso_id} className="curso-fila anima" style={{ '--i': indice } as CSSProperties}>
                      <Portada lenguaje={curso.lenguaje} variante={curso.curso_id} className="portada-mini" />
                      <div className="curso-fila-texto">
                        <a href={`/cursos/${curso.curso_id}/problemas`}>{curso.nombre}</a>
                        <BarraProgreso
                          valor={resumen.fraccion}
                          etiqueta={`Progreso en ${curso.nombre}: ${resumen.resueltos} de ${resumen.total} problemas resueltos`}
                        >
                          <span>
                            <strong>{resumen.resueltos}</strong> de {resumen.total} resueltos
                          </span>
                          <strong>{Math.round(resumen.fraccion * 100)} %</strong>
                        </BarraProgreso>
                      </div>
                    </li>
                  )
                })}
              </ul>
            </section>
          )}

          <section aria-labelledby="avance-actividad">
            <div className="seccion-cabecera">
              <h2 id="avance-actividad">Actividad reciente</h2>
            </div>
            <div className="tabla tabla-avance">
              <table>
                <thead>
                  <tr>
                    <th scope="col">Problema</th>
                    <th scope="col">Curso</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Envíos</th>
                    <th scope="col">Web</th>
                    <th scope="col">Móvil</th>
                    <th scope="col">Último envío</th>
                  </tr>
                </thead>
                <tbody>
                  {[...avance.problemas]
                    .sort((a, b) => Date.parse(b.ultimo_envio) - Date.parse(a.ultimo_envio))
                    .map((fila) => {
                      const conocido = porProblema.get(fila.problema_id)
                      return (
                        <tr key={fila.problema_id}>
                          <td>
                            {conocido ? (
                              <a href={`/problemas/${fila.problema_id}`}>{conocido.problema.titulo}</a>
                            ) : (
                              <a href={`/problemas/${fila.problema_id}`}>Problema {fila.problema_id}</a>
                            )}
                          </td>
                          <td>{conocido?.curso.nombre ?? '—'}</td>
                          <td>
                            {conocido ? (
                              <span className={`estado-curso estado-fila-${conocido.problema.estado}`}>
                                {etiquetaEstado(conocido.problema.estado)}
                              </span>
                            ) : (
                              '—'
                            )}
                          </td>
                          <td>{fila.envios}</td>
                          <td>{fila.desde_web}</td>
                          <td>{fila.desde_movil}</td>
                          <td>{new Date(fila.ultimo_envio).toLocaleString()}</td>
                        </tr>
                      )
                    })}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </main>
  )
}

export default Avance
