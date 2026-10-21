import './CasosPrueba.css'

// Mientras el docente no toque la lista, Formulario no tiene valor: se parte de un caso vacío.
const LISTA_INICIAL = [{ id: 1, entrada: '', salida_esperada: '' }]

// valor: lista de { id, entrada, salida_esperada }. id es solo de la pantalla (key estable) y no se envía.
// error: lista de admin-api con un objeto de errores por caso, en el orden de la lista.
function CasosPrueba({ id, etiqueta, valor, onCambio, error }) {
  const casos = valor || LISTA_INICIAL
  const errores = Array.isArray(error) ? error : []

  function cambiar(idCaso, nombre, texto) {
    // Sin trim: los espacios y saltos de línea son parte del caso.
    onCambio(casos.map((caso) => (caso.id === idCaso ? { ...caso, [nombre]: texto } : caso)))
  }

  function agregar() {
    // El siguiente id sale de la lista y no de un contador: así no se repite aunque el componente se vuelva a montar.
    const siguiente = Math.max(0, ...casos.map((caso) => caso.id)) + 1
    onCambio([...casos, { id: siguiente, entrada: '', salida_esperada: '' }])
  }

  function quitar(idCaso) {
    onCambio(casos.filter((caso) => caso.id !== idCaso))
  }

  return (
    <div id={id} role="group" aria-label={etiqueta} className="casos-prueba">
      {casos.map((caso, indice) => {
        const numero = indice + 1
        const erroresCaso = errores[indice] ?? {}
        return (
          <div key={caso.id} className="caso-prueba">
            {['entrada', 'salida_esperada'].map((nombre) => {
              const idCampo = `caso-${caso.id}-${nombre}`
              const mensaje = erroresCaso[nombre]?.join(' ')
              return (
                <div key={nombre} className="campo">
                  <label htmlFor={idCampo}>
                    {nombre === 'entrada' ? 'Entrada' : 'Salida esperada'} del caso {numero}
                  </label>
                  <textarea
                    id={idCampo}
                    rows={3}
                    value={caso[nombre]}
                    onChange={(evento) => cambiar(caso.id, nombre, evento.target.value)}
                    aria-invalid={mensaje ? 'true' : undefined}
                    aria-describedby={mensaje ? `${idCampo}-error` : undefined}
                  />
                  {mensaje && (
                    <p id={`${idCampo}-error`} className="error" role="alert">
                      {mensaje}
                    </p>
                  )}
                </div>
              )
            })}
            <button type="button" aria-label={`Quitar caso ${numero}`} onClick={() => quitar(caso.id)}>
              Quitar
            </button>
          </div>
        )
      })}
      <button type="button" onClick={agregar}>
        Agregar caso de prueba
      </button>
    </div>
  )
}

export default CasosPrueba
