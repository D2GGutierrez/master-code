import type { CSSProperties, ReactNode } from 'react'

// Progreso del estudiante. Los dos componentes reciben una fracción de 0 a 1 y una frase para los lectores de
// pantalla («3 de 8 problemas resueltos»): el dibujo nunca es lo único que dice el dato.

function limitar(valor: number) {
  return Number.isFinite(valor) ? Math.min(Math.max(valor, 0), 1) : 0
}

// Barra horizontal con una línea de texto opcional debajo (children).
export function BarraProgreso({ valor, etiqueta, children }: { valor: number; etiqueta: string; children?: ReactNode }) {
  const porcentaje = Math.round(limitar(valor) * 100)
  return (
    <div>
      <div
        className="progreso"
        role="progressbar"
        aria-label={etiqueta}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={porcentaje}
      >
        <span style={{ '--valor': `${porcentaje}%` } as CSSProperties} />
      </div>
      {children && <p className="progreso-texto">{children}</p>}
    </div>
  )
}

const RADIO = 42
const CIRCUNFERENCIA = 2 * Math.PI * RADIO

// Anillo con el porcentaje en el centro. El tamaño es una medida CSS («5rem»).
export function AnilloProgreso({ valor, etiqueta, tamano = '5rem' }: { valor: number; etiqueta: string; tamano?: string }) {
  const fraccion = limitar(valor)
  return (
    <div
      className="anillo"
      role="img"
      aria-label={etiqueta}
      style={{ '--tam': tamano, '--circ': CIRCUNFERENCIA.toFixed(2) } as CSSProperties}
    >
      <svg viewBox="0 0 100 100" aria-hidden="true" focusable="false">
        <circle className="anillo-pista" cx="50" cy="50" r={RADIO} />
        {/* Con 0 % no se dibuja el arco: un trazo de largo cero con extremo redondo se vería como un punto. */}
        {fraccion > 0 && (
          <circle
            className="anillo-valor"
            cx="50"
            cy="50"
            r={RADIO}
            strokeDasharray={CIRCUNFERENCIA}
            strokeDashoffset={CIRCUNFERENCIA * (1 - fraccion)}
          />
        )}
      </svg>
      <span className="anillo-texto" aria-hidden="true">
        {Math.round(fraccion * 100)}%
      </span>
    </div>
  )
}
