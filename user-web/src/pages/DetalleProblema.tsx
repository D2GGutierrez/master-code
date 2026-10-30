import { useEffect, useState } from 'react'
import { obtenerProblema, type ProblemaDetalle } from '../api/problemas.ts'
import Icono from '../components/Icono.tsx'
import { Hero } from '../components/Pantalla.tsx'
import { limpiarEnunciado } from '../enunciado.ts'
import { etiquetaDificultad, etiquetaTipoEjercicio } from '../etiquetas.ts'
import { claveLenguaje, etiquetaLenguaje } from '../lenguajes.ts'
import './DetalleProblema.css'

// Solo la escritura de código usa el editor (HU32-T3); la misma regla que en EditorProblema.tsx.
const TIPO_CON_EDITOR = 'escritura_codigo'

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

// HU32-T2: solo lectura. El botón «Ir al editor» es HU32-T3.
function DetalleProblema({ id }: { id: number }) {
  const [problema, setProblema] = useState<ProblemaDetalle | null>(null)
  // 403 y 404 tienen su mensaje y no se reintentan; cualquier otro fallo permite «Reintentar».
  const [sinAcceso, setSinAcceso] = useState('')
  const [error, setError] = useState(false)
  // Cambiarlo vuelve a ejecutar el efecto de carga.
  const [recargas, setRecargas] = useState(0)

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

  const volver = problema ? (
    <a href={`/cursos/${problema.curso_id}/problemas`}>Volver a los problemas del curso</a>
  ) : (
    <a href="/inicio">Volver a Mis cursos</a>
  )

  if (sinAcceso || error || !problema) {
    return (
      <main className="detalle-problema">
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

  const enunciado = limpiarEnunciado(problema.enunciado)
  const conEjemplos = problema.plataforma !== 'movil' && problema.casos_prueba.length > 0
  return (
    <main className="detalle-problema">
      <Hero clase={`hero-compacto hero-curso leng-${claveLenguaje(problema.lenguaje)}`}>
        <p className="hero-volver">
          <a href={`/cursos/${problema.curso_id}/problemas`}>
            <Icono nombre="atras" />
            Volver a los problemas del curso
          </a>
        </p>
        <h1>{problema.titulo}</h1>
        <p className="hero-meta">
          <span className="hero-etiqueta">{etiquetaDificultad(problema.dificultad)}</span>
          <span className="hero-etiqueta">{etiquetaTipoEjercicio(problema.tipo_ejercicio)}</span>
          <span className="hero-etiqueta">{etiquetaLenguaje(problema.lenguaje)}</span>
        </p>
        {problema.plataforma !== 'movil' && problema.tipo_ejercicio === TIPO_CON_EDITOR && (
          <div className="hero-acciones">
            <a className="boton-hero" href={`/problemas/${problema.id}/editor`}>
              Ir al editor de código
              <Icono nombre="flecha" />
            </a>
          </div>
        )}
      </Hero>
      {problema.plataforma === 'movil' ? (
        <p>Este problema es solo para la app móvil.</p>
      ) : (
        <>
          {problema.tipo_ejercicio !== TIPO_CON_EDITOR && <p>Este problema no usa el editor de código.</p>}
          <div className={conEjemplos ? 'problema-lectura con-ejemplos' : 'problema-lectura'}>
            <section className="lectura">
              <h2>Enunciado</h2>
              {'html' in enunciado ? (
                // Único HTML insertado en user-web: ya pasó por DOMPurify en limpiarEnunciado.
                <div className="enunciado" dangerouslySetInnerHTML={{ __html: enunciado.html }} />
              ) : (
                <p className="enunciado enunciado-texto">{enunciado.texto}</p>
              )}
            </section>
            {conEjemplos && (
              <section className="lectura">
                <h2>Ejemplos</h2>
                {problema.casos_prueba.map((caso, indice) => (
                  // Los casos no traen id; el orden lo fija el servidor y no cambia en esta pantalla.
                  <div key={indice} className="ejemplo">
                    <h3>Ejemplo {indice + 1}</h3>
                    <p className="ejemplo-rotulo">Entrada</p>
                    <pre>{caso.entrada}</pre>
                    <p className="ejemplo-rotulo">Salida esperada</p>
                    <pre>{caso.salida_esperada}</pre>
                  </div>
                ))}
              </section>
            )}
          </div>
        </>
      )}
    </main>
  )
}

export default DetalleProblema
