import { useId, useState } from 'react'
import { DIFICULTADES, PLATAFORMAS, TIPOS_EJERCICIO } from '../pages/NuevoProblema.jsx'
import { agregarVisibles, alternarSeleccion, filtrarPorTitulo, ordenarComoBanco } from '../seleccionProblemas.js'
import EnlaceBandeja from './EnlaceBandeja.jsx'
import './SelectorProblemas.css'

// admin-api guarda valores internos (p. ej. escritura_codigo); un valor desconocido se muestra tal cual.
function etiqueta(opciones, valor) {
  return opciones.find((opcion) => opcion.valor === valor)?.etiqueta ?? valor
}

function textoContador(cantidad) {
  if (cantidad === 0) {
    return 'Ningún problema seleccionado'
  }
  return cantidad === 1 ? '1 problema seleccionado' : `${cantidad} problemas seleccionados`
}

// HU62-T2 (CA-046.1, CA-046.2): elegir varios problemas del banco de un curso para una evaluación. Controlado: no pide
// datos. problemas son los ya aprobados del curso; omitidos, cuántos del curso quedaron fuera por no estar aprobados.
// alCambiar recibe siempre los ids en el orden del banco y sin repetir. HU63-T2 agregará aquí los puntos.
function SelectorProblemas({ problemas, seleccionados, alCambiar, error, omitidos = 0 }) {
  // Solo de la pantalla: buscar no cambia la selección.
  const [busqueda, setBusqueda] = useState('')
  const id = useId()
  const idBuscar = `${id}-buscar`
  const idError = `${id}-error`
  const visibles = filtrarPorTitulo(problemas, busqueda)
  const elegidos = ordenarComoBanco(problemas, seleccionados)

  function cambiar(ids) {
    alCambiar(ordenarComoBanco(problemas, ids))
  }

  return (
    <fieldset
      className="selector-problemas"
      aria-invalid={error ? 'true' : undefined}
      aria-describedby={error ? idError : undefined}
    >
      <legend>Problemas de la evaluación</legend>
      {problemas.length === 0 ? (
        <p>
          Este curso aún no tiene problemas aprobados. Aprueba problemas desde la bandeja de revisión para armar una
          evaluación.
        </p>
      ) : (
        <>
          <div className="selector-busqueda">
            <label htmlFor={idBuscar}>Buscar problema</label>
            <input id={idBuscar} type="search" value={busqueda} onChange={(evento) => setBusqueda(evento.target.value)} />
          </div>
          <div className="selector-acciones">
            <button type="button" onClick={() => cambiar(agregarVisibles(seleccionados, visibles.map((p) => p.id)))}>
              Seleccionar los visibles
            </button>
            <button type="button" onClick={() => cambiar([])}>
              Quitar selección
            </button>
            <p className="selector-contador" aria-live="polite">
              {textoContador(elegidos.length)}
            </p>
          </div>
          {visibles.length === 0 ? (
            <p>Ningún problema coincide con la búsqueda.</p>
          ) : (
            <ul className="selector-lista">
              {visibles.map((problema) => (
                <li key={problema.id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={seleccionados.includes(problema.id)}
                      onChange={() => cambiar(alternarSeleccion(seleccionados, problema.id))}
                    />
                    <span className="selector-titulo">{problema.titulo}</span>
                    <span className="selector-detalle">
                      {etiqueta(DIFICULTADES, problema.dificultad)} · {etiqueta(TIPOS_EJERCICIO, problema.tipo_ejercicio)}
                      {problema.plataforma !== 'ambas' && ` · ${etiqueta(PLATAFORMAS, problema.plataforma)}`}
                    </span>
                  </label>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      {omitidos > 0 && (
        <p className="selector-nota">
          {omitidos === 1
            ? '1 problema del curso no aparece porque aún no está aprobado.'
            : `${omitidos} problemas del curso no aparecen porque aún no están aprobados.`}{' '}
          <EnlaceBandeja />
        </p>
      )}
      {error && (
        <p id={idError} className="error" role="alert">
          {error}
        </p>
      )}
    </fieldset>
  )
}

export default SelectorProblemas
