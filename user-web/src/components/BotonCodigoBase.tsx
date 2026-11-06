import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import './BotonCodigoBase.css'

type Props = {
  codigoBase: string | null | undefined
  codigoActual: string
  alRestaurar: () => void
}

// HU34-T4 (D8): vuelve a poner en el editor el código base del problema, tras confirmar. La confirmación es en línea,
// como la de «Enviar solución». No guarda nada: el autoguardado toma el cambio como cualquier otro.
function BotonCodigoBase({ codigoBase, codigoActual, alRestaurar }: Props) {
  const [confirmando, setConfirmando] = useState(false)
  const [anuncio, setAnuncio] = useState('')
  const fila = useRef<HTMLDivElement>(null)
  const boton = useRef<HTMLButtonElement>(null)
  const cancelar = useRef<HTMLButtonElement>(null)
  // Un segundo clic en el mismo instante llega antes de que React quite la confirmación: esto lo frena.
  const restaurado = useRef(false)
  // Para no mover el foco al montar: solo al cerrar una confirmación que se abrió.
  const seAbrio = useRef(false)
  const id = useId()

  useEffect(() => {
    if (confirmando) {
      cancelar.current?.focus()
    } else if (seAbrio.current) {
      // Tras restaurar, el botón queda deshabilitado (ya no hay nada que restaurar) y no puede recibir el foco: va a
      // la fila, que es lo más cercano.
      if (boton.current && !boton.current.disabled) {
        boton.current.focus()
      } else {
        fila.current?.focus()
      }
    }
  }, [confirmando])

  if (typeof codigoBase !== 'string' || !codigoBase.trim()) {
    return null
  }

  function abrir() {
    restaurado.current = false
    seAbrio.current = true
    setAnuncio('')
    setConfirmando(true)
  }

  function confirmar() {
    if (restaurado.current) {
      return
    }
    restaurado.current = true
    alRestaurar()
    setAnuncio('Código base restaurado.')
    setConfirmando(false)
  }

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === 'Escape') {
      setConfirmando(false)
    }
  }

  return (
    <div className="codigo-base" ref={fila} tabIndex={-1}>
      {confirmando ? (
        <div
          className="codigo-base-confirmacion"
          role="group"
          aria-labelledby={`${id}-titulo`}
          aria-describedby={`${id}-texto`}
          onKeyDown={alTeclear}
        >
          <p id={`${id}-titulo`} className="codigo-base-titulo">
            ¿Volver al código base original?
          </p>
          <p id={`${id}-texto`}>
            Se reemplazará el código que hay ahora en el editor por el código base del problema. Si tienes un borrador
            guardado, también se reemplazará.
          </p>
          {/* «Cancelar» recibe el foco: un Enter por accidente no borra lo escrito. */}
          <button ref={cancelar} type="button" onClick={() => setConfirmando(false)}>
            Cancelar
          </button>{' '}
          <button type="button" className="boton-primario" onClick={confirmar}>
            Volver al código base
          </button>
        </div>
      ) : (
        <button ref={boton} type="button" onClick={abrir} disabled={codigoActual === codigoBase}>
          Volver al código base original
        </button>
      )}
      <p className="solo-lectores" role="status">
        {anuncio}
      </p>
    </div>
  )
}

export default BotonCodigoBase
