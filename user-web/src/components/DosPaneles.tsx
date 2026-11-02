import { useRef, useState, type CSSProperties, type KeyboardEvent, type PointerEvent, type ReactNode } from 'react'
import {
  DIVISION_MAXIMA,
  DIVISION_MINIMA,
  DIVISION_POR_DEFECTO,
  divisionDesdePuntero,
  limitarDivision,
  moverDivision,
} from '../division.ts'
import './DosPaneles.css'

type Props = {
  izquierdo: ReactNode
  derecho: ReactNode
  divisionInicial?: number
  // Se llama al soltar el divisor y con cada tecla que lo movió (HU33-T4 lo usa para recordar la posición).
  alCambiarDivision?: (division: number) => void
}

const TECLAS_DIVISOR = ['ArrowLeft', 'ArrowRight', 'Home', 'End']

// HU33-T1: dos paneles con un divisor que se arrastra (mouse o dedo) o se mueve con el teclado (patrón «window
// splitter»). Bajo 768 px el CSS los apila y oculta el divisor.
function DosPaneles({ izquierdo, derecho, divisionInicial = DIVISION_POR_DEFECTO, alCambiarDivision }: Props) {
  const contenedor = useRef<HTMLDivElement>(null)
  const [division, setDivision] = useState(() => limitarDivision(divisionInicial))
  const [arrastrando, setArrastrando] = useState(false)

  function cambiar(nueva: number) {
    if (nueva !== division) {
      setDivision(nueva)
      alCambiarDivision?.(nueva)
    }
  }

  function alPresionar(evento: PointerEvent<HTMLDivElement>) {
    if (evento.button !== 0) {
      return
    }
    // Con el puntero capturado, el arrastre sigue aunque pase sobre Monaco o salga de la ventana.
    evento.currentTarget.setPointerCapture(evento.pointerId)
    setArrastrando(true)
  }

  function alMover(evento: PointerEvent<HTMLDivElement>) {
    if (!arrastrando || !contenedor.current) {
      return
    }
    const caja = contenedor.current.getBoundingClientRect()
    setDivision(divisionDesdePuntero(evento.clientX, caja.left, caja.width))
  }

  // Avisa una sola vez al terminar el arrastre, no en cada píxel.
  function alSoltar() {
    if (arrastrando) {
      setArrastrando(false)
      alCambiarDivision?.(division)
    }
  }

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (!TECLAS_DIVISOR.includes(evento.key)) {
      return
    }
    // Sin esto, Home y End también desplazan la página.
    evento.preventDefault()
    cambiar(moverDivision(division, evento.key))
  }

  return (
    <div
      ref={contenedor}
      className={arrastrando ? 'dos-paneles arrastrando' : 'dos-paneles'}
      style={{ '--izq': `${division}%` } as CSSProperties}
    >
      <div className="dos-paneles-panel">{izquierdo}</div>
      <div
        className="dos-paneles-divisor"
        role="separator"
        aria-orientation="vertical"
        aria-label="Cambiar el ancho de los paneles"
        aria-valuenow={Math.round(division)}
        aria-valuemin={DIVISION_MINIMA}
        aria-valuemax={DIVISION_MAXIMA}
        tabIndex={0}
        onPointerDown={alPresionar}
        onPointerMove={alMover}
        onPointerUp={alSoltar}
        onPointerCancel={alSoltar}
        onKeyDown={alTeclear}
      />
      <div className="dos-paneles-panel">{derecho}</div>
    </div>
  )
}

export default DosPaneles
