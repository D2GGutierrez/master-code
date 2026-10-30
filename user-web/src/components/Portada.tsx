import type { ReactNode } from 'react'
import { estrella } from '../dibujo.ts'
import { claveLenguaje, type ClaveLenguaje } from '../lenguajes.ts'

// La «imagen» de un curso: un degradado con los colores de su lenguaje, una ventana de código dibujada y el
// monograma del lenguaje. Es un SVG propio (sin imágenes ni dependencias) y decorativo: el nombre y el lenguaje
// ya están escritos al lado. Hay tres variantes por lenguaje y se elige con el número del curso, así dos cursos
// del mismo lenguaje no se ven idénticos.

const GLIFOS: Record<ClaveLenguaje, string> = { python: 'Py', java: 'Jv', javascript: 'JS', otro: '</>' }

// Líneas de «código»: [sangría, largo, resaltada].
const LINEAS: Record<ClaveLenguaje, [number, number, boolean][]> = {
  python: [[14, 70, true], [14, 46, false], [30, 66, false], [30, 38, true], [46, 58, false], [14, 34, false]],
  java: [[14, 84, true], [26, 60, false], [26, 76, false], [38, 50, true], [26, 32, false], [14, 20, false]],
  javascript: [[14, 52, true], [14, 92, false], [26, 62, false], [26, 44, true], [14, 74, false], [14, 28, false]],
  otro: [[14, 58, true], [26, 84, false], [26, 50, false], [14, 70, true], [26, 40, false], [14, 26, false]],
}

// El motivo del fondo, distinto en cada lenguaje: ondas, bloques, diagonales o puntos.
const MOTIVOS: Record<ClaveLenguaje, ReactNode> = {
  python: (
    <>
      <path className="pt-trazo" d="M-10 150C40 120 70 176 130 146S230 106 330 138" />
      <path className="pt-trazo pt-trazo-tenue" d="M-10 168C50 138 90 194 150 164S250 124 330 156" />
    </>
  ),
  java: (
    <>
      {[0, 1, 2].flatMap((fila) =>
        [0, 1, 2, 3, 4].map((columna) => (
          <rect
            key={`${fila}-${columna}`}
            className={(fila + columna) % 3 === 0 ? 'pt-medio' : 'pt-tenue'}
            x={16 + columna * 17}
            y={14 + fila * 15}
            width="12"
            height="10"
            rx="3"
          />
        )),
      )}
    </>
  ),
  javascript: (
    <>
      <path className="pt-trazo" d="M0 70L70 0M22 92L114 0M56 100L156 0" />
      <circle className="pt-tenue" cx="40" cy="132" r="14" />
    </>
  ),
  otro: (
    <>
      {[0, 1, 2].flatMap((fila) =>
        [0, 1, 2, 3, 4, 5].map((columna) => (
          <circle key={`${fila}-${columna}`} className="pt-medio" cx={20 + columna * 16} cy={18 + fila * 16} r="2.2" />
        )),
      )}
    </>
  ),
}

// Dónde va la ventana ([x, y, giro]), dónde el monograma ([x, y, anclaje]) y cómo se refleja el motivo.
const VARIANTES = [
  { ventana: [150, 34, -5], glifo: [12, 168, 'start'], espejo: '' },
  { ventana: [20, 40, 4], glifo: [308, 168, 'end'], espejo: 'translate(320 0) scale(-1 1)' },
  { ventana: [158, 48, 7], glifo: [12, 92, 'start'], espejo: 'translate(0 180) scale(1 -1)' },
] as const

export function Portada({ lenguaje, variante = 0, className }: { lenguaje: string; variante?: number; className?: string }) {
  const clave = claveLenguaje(lenguaje)
  const indice = Math.abs(Math.trunc(variante)) % VARIANTES.length
  const { ventana, glifo, espejo } = VARIANTES[indice]
  return (
    <div
      className={`portada leng-${clave} portada-v${indice}${className ? ` ${className}` : ''}`}
      aria-hidden="true"
    >
      <svg viewBox="0 0 320 180" preserveAspectRatio="xMidYMid slice" focusable="false">
        <g transform={espejo || undefined}>{MOTIVOS[clave]}</g>
        <text
          className="pt-glifo"
          x={glifo[0]}
          y={glifo[1]}
          textAnchor={glifo[2]}
          style={clave === 'otro' ? { fontSize: 70 } : undefined}
        >
          {GLIFOS[clave]}
        </text>
        <g transform={`translate(${ventana[0]} ${ventana[1]}) rotate(${ventana[2]})`}>
          <rect className="pt-ventana" width="152" height="104" rx="12" />
          <path className="pt-linea" d="M0 24H152" />
          <circle className="pt-claro" cx="14" cy="12" r="3.5" />
          <circle className="pt-medio" cx="26" cy="12" r="3.5" />
          <circle className="pt-medio" cx="38" cy="12" r="3.5" />
          {LINEAS[clave].map(([sangria, largo, resaltada], fila) => (
            <rect
              key={fila}
              className={resaltada ? 'pt-claro' : 'pt-medio'}
              x={sangria}
              y={36 + fila * 11}
              width={largo}
              height="6"
              rx="3"
            />
          ))}
        </g>
        <path className="pt-claro" d={estrella(292, 24, 7)} />
        <path className="pt-medio" d={estrella(136, 24, 4)} />
        <circle className="pt-claro" cx="300" cy="150" r="2.5" />
      </svg>
    </div>
  )
}
