import { useEffect, useState } from 'react'
import { obtenerProblema, type ProblemaDetalle } from '../api/problemas.ts'
import { limpiarEnunciado } from '../enunciado.ts'
import { etiquetaDificultad, etiquetaTipoEjercicio } from '../etiquetas.ts'
import { etiquetaLenguaje } from '../lenguajes.ts'
import './DetalleProblema.css'

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
  return (
    <main className="detalle-problema">
      <p>{volver}</p>
      <h1>{problema.titulo}</h1>
      <p>
        <span className="etiqueta-problema">{etiquetaDificultad(problema.dificultad)}</span>{' '}
        <span className="etiqueta-problema">{etiquetaTipoEjercicio(problema.tipo_ejercicio)}</span>{' '}
        <span className="etiqueta-problema">{etiquetaLenguaje(problema.lenguaje)}</span>
      </p>
      {problema.plataforma === 'movil' ? (
        <p>Este problema es solo para la app móvil.</p>
      ) : (
        <>
          <section>
            <h2>Enunciado</h2>
            {'html' in enunciado ? (
              // Único HTML insertado en user-web: ya pasó por DOMPurify en limpiarEnunciado.
              <div className="enunciado" dangerouslySetInnerHTML={{ __html: enunciado.html }} />
            ) : (
              <p className="enunciado enunciado-texto">{enunciado.texto}</p>
            )}
          </section>
          {problema.casos_prueba.length > 0 && (
            <section>
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
        </>
      )}
    </main>
  )
}

export default DetalleProblema
