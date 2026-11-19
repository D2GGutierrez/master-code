import { useState } from 'react'
import './Formulario.css'

// «acciones» (opcional) cambia el botón único por varios, cada uno { valor, texto, secundaria }: onEnviar recibe como
// segundo argumento el valor del botón que se pulsó (p. ej. «Guardar cambios» y «Guardar y aprobar»).
function Formulario({ campos, textoBoton, enviando, errores, onEnviar, valoresIniciales, onCambio, acciones }) {
  const [valores, setValores] = useState(valoresIniciales ?? {})
  // Campos que el docente cambió desde los últimos errores: su error ya no se muestra.
  const [editados, setEditados] = useState({})
  const [erroresPrevios, setErroresPrevios] = useState(errores)
  // Cada envío trae un objeto errores nuevo: entonces todos los errores vuelven a verse.
  if (errores !== erroresPrevios) {
    setErroresPrevios(errores)
    setEditados({})
  }
  // Un campo sin mostrarSi se ve siempre; con mostrarSi, solo cuando devuelve true según los valores actuales.
  const visibles = campos.filter((campo) => !campo.mostrarSi || campo.mostrarSi(valores))

  function manejarCambio(evento) {
    setValores({ ...valores, [evento.target.name]: evento.target.value })
    setEditados({ ...editados, [evento.target.name]: true })
    // Opcional: avisa al padre de que el docente cambió algo (p. ej. para borrar un mensaje).
    onCambio?.()
  }

  function manejarEnvio(evento) {
    // Evita que el navegador recargue la página al enviar.
    evento.preventDefault()
    // Lo escrito en un campo oculto sigue en el estado, pero solo se envían los campos visibles.
    const enviados = {}
    for (const campo of visibles) {
      if (campo.nombre in valores) {
        enviados[campo.nombre] = valores[campo.nombre]
      }
    }
    onEnviar(enviados, evento.nativeEvent?.submitter?.value || undefined)
  }

  return (
    <form className="formulario" onSubmit={manejarEnvio}>
      {errores.general && <p className="error">{errores.general}</p>}
      {visibles.map((campo) => {
        const error = !editados[campo.nombre] && errores[campo.nombre]
        const idError = `${campo.nombre}-error`
        return (
          <div key={campo.nombre} className="campo">
            <label htmlFor={campo.nombre}>{campo.etiqueta}</label>
            {/* Un campo puede traer su propio componente (p. ej. un editor) si no es un input. */}
            {campo.componente ? (
              <campo.componente
                id={campo.nombre}
                etiqueta={campo.etiqueta}
                valor={valores[campo.nombre] ?? ''}
                error={error}
                // Forma funcional: el componente puede guardar este callback de un render anterior.
                onCambio={(valor) => {
                  setValores((anteriores) => ({ ...anteriores, [campo.nombre]: valor }))
                  setEditados((anteriores) => ({ ...anteriores, [campo.nombre]: true }))
                  onCambio?.()
                }}
              />
            ) : campo.tipo === 'select' ? (
              <select
                id={campo.nombre}
                name={campo.nombre}
                value={valores[campo.nombre] ?? ''}
                onChange={manejarCambio}
                required
                aria-invalid={error ? 'true' : undefined}
                aria-describedby={error ? idError : undefined}
              >
                {/* Opción vacía: con required obliga a elegir una de verdad. */}
                <option value="">Selecciona…</option>
                {campo.opciones.map((opcion) => (
                  <option key={opcion.valor} value={opcion.valor}>
                    {opcion.etiqueta}
                  </option>
                ))}
              </select>
            ) : (
              <input
                id={campo.nombre}
                name={campo.nombre}
                type={campo.tipo}
                min={campo.min}
                max={campo.max}
                value={valores[campo.nombre] ?? ''}
                onChange={manejarCambio}
                autoComplete={campo.autoComplete}
                inputMode={campo.inputMode}
                maxLength={campo.maxLength}
                required
                aria-invalid={error ? 'true' : undefined}
                aria-describedby={[campo.ayuda ? `${campo.nombre}-ayuda-campo` : '', error ? idError : ''].filter(Boolean).join(' ') || undefined}
              />
            )}
            {/* Una línea de ayuda bajo el campo (qué se espera escribir). */}
            {campo.ayuda && !campo.componente && (
              <p id={`${campo.nombre}-ayuda-campo`} className="campo-ayuda">
                {campo.ayuda}
              </p>
            )}
            {/* Una lista (errores por caso de prueba) la pinta el propio componente. */}
            {typeof error === 'string' && (
              <p id={idError} className="error" role="alert">
                {error}
              </p>
            )}
          </div>
        )
      })}
      {acciones ? (
        <div className="formulario-acciones">
          {acciones.map((accion) => (
            <button
              key={accion.valor}
              type="submit"
              name="accion"
              value={accion.valor}
              className={accion.secundaria ? 'boton-secundario' : undefined}
              disabled={enviando}
            >
              {accion.texto}
            </button>
          ))}
        </div>
      ) : (
        <button type="submit" disabled={enviando}>
          {textoBoton}
        </button>
      )}
    </form>
  )
}

export default Formulario
