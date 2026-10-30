import { useEffect, useState } from 'react'
import { obtenerAvance, type Avance } from './api/avance.ts'
import type { MiCurso } from './api/cursos.ts'
import { listarProblemas } from './api/problemas.ts'
import type { ProblemasPorCurso } from './progreso.ts'

export type DatosProgreso = {
  problemas: ProblemasPorCurso
  // null mientras llega o si falló.
  avance: Avance | null
  // Ya terminaron todas las peticiones, hayan salido bien o mal.
  listo: boolean
}

// Lo que se fue recibiendo para una lista de cursos concreta («para»): si la lista cambia, lo anterior ya no vale.
type Recibido = { para: MiCurso[] | null; problemas: ProblemasPorCurso; avance: Avance | null; faltan: number }

const NADA: Recibido = { para: null, problemas: new Map(), avance: null, faltan: 0 }

// Para los cursos dados, trae la lista de problemas de cada uno (con el estado de cada problema) y, si se pide, el
// avance global. Las respuestas llegan una a una y la pantalla muestra lo que ya hay. El progreso es un dato extra:
// si una petición falla, ese dato no se muestra y nada más se rompe.
export function useProgreso(cursos: MiCurso[] | null, conAvance = true): DatosProgreso {
  const [recibido, setRecibido] = useState<Recibido>(NADA)

  useEffect(() => {
    if (!cursos) {
      return
    }
    // Si cambian los cursos o se sale de la pantalla, las respuestas viejas se descartan.
    let activo = true
    let problemas: ProblemasPorCurso = new Map()
    let avance: Avance | null = null
    let faltan = cursos.length + (conAvance ? 1 : 0)
    const terminar = () => {
      faltan -= 1
      if (activo) {
        setRecibido({ para: cursos, problemas, avance, faltan })
      }
    }
    for (const curso of cursos) {
      listarProblemas(curso.curso_id)
        .then(
          (respuesta) => {
            problemas = new Map(problemas).set(curso.curso_id, respuesta.ok ? respuesta.problemas : null)
          },
          () => {
            problemas = new Map(problemas).set(curso.curso_id, null)
          },
        )
        .finally(terminar)
    }
    if (conAvance) {
      obtenerAvance()
        .then(
          (datos) => {
            avance = datos
          },
          () => {},
        )
        .finally(terminar)
    }
    return () => {
      activo = false
    }
  }, [cursos, conAvance])

  const vigente = cursos !== null && recibido.para === cursos
  const sinPeticiones = cursos !== null && cursos.length === 0 && !conAvance
  return {
    problemas: vigente ? recibido.problemas : NADA.problemas,
    avance: vigente ? recibido.avance : null,
    listo: sinPeticiones || (vigente && recibido.faltan === 0),
  }
}
