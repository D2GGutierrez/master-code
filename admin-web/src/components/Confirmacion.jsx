import { useEffect, useRef } from 'react'
import './Confirmacion.css'

// <dialog> nativo: showModal() atrapa el foco y bloquea la página sin librerías.
function Confirmacion({ abierto, titulo, children, textoConfirmar, textoCancelar, enviando, onConfirmar, onCancelar }) {
  const dialogo = useRef(null)

  useEffect(() => {
    if (!abierto) {
      return
    }
    const elemento = dialogo.current
    elemento.showModal()
    // Cerrar en la limpieza evita llamar dos veces a showModal (error) cuando el efecto se repite.
    return () => elemento.close()
  }, [abierto])

  function manejarEsc(evento) {
    // Esc cerraría el dialog por su cuenta; así lo decide el padre con abierto, y no mientras se guarda.
    evento.preventDefault()
    if (!enviando) {
      onCancelar()
    }
  }

  return (
    <dialog ref={dialogo} aria-labelledby="confirmacion-titulo" onCancel={manejarEsc}>
      <h2 id="confirmacion-titulo">{titulo}</h2>
      {children}
      <div className="acciones-dialogo">
        <button type="button" className="boton-primario" disabled={enviando} onClick={onConfirmar}>
          {enviando ? 'Guardando…' : textoConfirmar}
        </button>
        <button type="button" disabled={enviando} onClick={onCancelar}>
          {textoCancelar}
        </button>
      </div>
    </dialog>
  )
}

export default Confirmacion
