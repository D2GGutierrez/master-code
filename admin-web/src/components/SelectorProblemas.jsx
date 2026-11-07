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
// alCambiar recibe siempre los ids en el orden del banco y sin repetir.
// HU63-T2 (CA-047.1, CA-047.2): con puntos ({ id: texto }), cada problema marcado lleva su campo de puntos;
// alCambiarPuntos(id, texto) recibe lo escrito y erroresPuntos ({ id: mensaje }) se muestra bajo cada campo. Sin
// puntos, el selector es el de HU62-T2.
function SelectorProblemas({
  problemas,
  seleccionados,
  alCambiar,
  error,
  omitidos = 0,
  puntos,
  alCambiarPuntos,
  erroresPuntos = {},
}) {
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
              {visibles.map((problema) => {
                const marcado = seleccionados.includes(problema.id)
                const errorPuntos = erroresPuntos[problema.id]
                // Sin useId: la pantalla lleva el foco al primer campo de puntos con error por este id.
                const idPuntos = `puntos-${problema.id}`
                return (
                  <li key={problema.id} className={puntos ? 'selector-fila' : undefined}>
                    <label>
                      <input
                        type="checkbox"
                        checked={marcado}
                        onChange={() => cambiar(alternarSeleccion(seleccionados, problema.id))}
                      />
                      <span className="selector-titulo">{problema.titulo}</span>
                      <span className="selector-detalle">
                        {etiqueta(DIFICULTADES, problema.dificultad)} ·{' '}
                        {etiqueta(TIPOS_EJERCICIO, problema.tipo_ejercicio)}
                        {problema.plataforma !== 'ambas' && ` · ${etiqueta(PLATAFORMAS, problema.plataforma)}`}
                      </span>
                    </label>
                    {puntos && marcado && (
                      <div className="selector-puntos">
                        {/* Texto y no number: «2,5» llega tal cual y un error se explica, sin vaciar el campo. */}
                        <input
                          id={idPuntos}
                          type="text"
                          inputMode="decimal"
                          value={puntos[problema.id] ?? ''}
                          onChange={(evento) => alCambiarPuntos(problema.id, evento.target.value)}
                          aria-label={`Puntos de ${problema.titulo}`}
                          aria-invalid={errorPuntos ? 'true' : undefined}
                          aria-describedby={errorPuntos ? `${idPuntos}-error` : undefined}
                        />
                        <span aria-hidden="true">pts</span>
                        {errorPuntos && (
                          <p id={`${idPuntos}-error`} className="error">
                            {errorPuntos}
                          </p>
                        )}
                      </div>
                    )}
                  </li>
                )
              })}
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
