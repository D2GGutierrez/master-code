import type { CSSProperties, ReactNode } from 'react'
import { estadoDelCurso, type EstadoCurso, type Resumen } from '../progreso.ts'
import { claveLenguaje, etiquetaLenguaje } from '../lenguajes.ts'
import Icono from './Icono.tsx'
import { Portada } from './Portada.tsx'
import { BarraProgreso } from './Progreso.tsx'

type DatosCurso = { curso_id: number; nombre: string; lenguaje: string; institucion: string }

const ESTADOS: Record<EstadoCurso, { texto: string; clase: string; accion: string }> = {
  'sin-problemas': { texto: 'Sin problemas aún', clase: 'estado-curso estado-curso-nuevo', accion: 'Abrir curso' },
  nuevo: { texto: 'Sin empezar', clase: 'estado-curso estado-curso-nuevo', accion: 'Empezar' },
  'en-curso': { texto: 'En curso', clase: 'estado-curso', accion: 'Continuar' },
  completo: { texto: 'Completado', clase: 'estado-curso estado-curso-completo', accion: 'Repasar' },
}

// Tarjeta de un curso: la portada, el lenguaje, el nombre, la institución y, si se conoce, el progreso.
//  - progreso undefined: no se muestra nada (p. ej. al buscar cursos, donde no hay avance).
//  - progreso 'cargando': marcador con brillo mientras llega.
//  - progreso null: no se pudo cargar; la tarjeta funciona igual, sin barra.
// Con «enlace» toda la tarjeta lleva a ese destino; sin él, el pie lo pone quien la usa.
export function TarjetaCurso({
  curso,
  indice,
  enlace,
  detalle,
  progreso,
  pie,
}: {
  curso: DatosCurso
  indice: number
  enlace?: string
  detalle?: string
  progreso?: Resumen | 'cargando' | null
  pie?: ReactNode
}) {
  const resumen = typeof progreso === 'object' && progreso !== null ? progreso : null
  const estado = resumen ? ESTADOS[estadoDelCurso(resumen)] : null
  const contenido = (
    <>
      <Portada lenguaje={curso.lenguaje} variante={curso.curso_id} />
      <div className="tarjeta-cuerpo">
        <p className="tarjeta-etiquetas">
          <span className={`chip-leng leng-${claveLenguaje(curso.lenguaje)}`}>{etiquetaLenguaje(curso.lenguaje)}</span>
          {estado && <span className={estado.clase}>{estado.texto}</span>}
        </p>
        <h3>{curso.nombre}</h3>
        <p>{detalle ? `${curso.institucion} · ${detalle}` : curso.institucion}</p>
        {resumen && resumen.total > 0 && (
          <BarraProgreso
            valor={resumen.fraccion}
            etiqueta={`Progreso en ${curso.nombre}: ${resumen.resueltos} de ${resumen.total} problemas resueltos`}
          >
            <span>
              <strong>{resumen.resueltos}</strong> de {resumen.total} resueltos
            </span>
            <strong>{Math.round(resumen.fraccion * 100)} %</strong>
          </BarraProgreso>
        )}
        {progreso === 'cargando' && (
          <div aria-hidden="true">
            <div className="esqueleto" style={{ height: '0.5rem', borderRadius: 999 }} />
            <div className="esqueleto" style={{ height: '0.75rem', width: '45%', marginTop: '0.5rem' }} />
          </div>
        )}
        {enlace ? (
          <div className="tarjeta-pie">
            <span>{estado?.accion ?? 'Abrir curso'}</span>
            <Icono nombre="flecha" />
          </div>
        ) : (
          pie && <div className="tarjeta-pie">{pie}</div>
        )}
      </div>
    </>
  )
  return (
    <li className="tarjeta-curso anima" style={{ '--i': indice } as CSSProperties}>
      {enlace ? (
        <a className="tarjeta-curso-enlace" href={enlace}>
          {contenido}
        </a>
      ) : (
        <div className="tarjeta-curso-enlace">{contenido}</div>
      )}
    </li>
  )
}
