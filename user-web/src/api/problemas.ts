import { pedirConSesion } from './cliente.ts'

// Mismos campos que el record ProblemaDeLista de user-api (GET /api/cursos/{id}/problemas), en snake_case.
export type ProblemaDeLista = {
  id: number
  titulo: string
  lenguaje: string
  dificultad: string
  plataforma: string
  tipo_ejercicio: string
  fecha_publicacion: string
  estado: 'sin_abrir' | 'intentado' | 'resuelto'
}

export type RespuestaProblemas = { ok: true; problemas: ProblemaDeLista[] } | { ok: false; status: number }

// HU30: vacío = sin filtro (no se envía el parámetro). Valores exactos de user-api.
export type FiltrosProblemas = { dificultad?: string; estado?: string }

// plataforma=web: user-api deja los de la web y los de ambas; los solo móviles no se pueden resolver aquí.
// signal permite cancelar una petición que ya no sirve (el estudiante cambió el filtro antes de la respuesta).
export async function listarProblemas(
  cursoId: number,
  filtros: FiltrosProblemas = {},
  signal?: AbortSignal,
): Promise<RespuestaProblemas> {
  const consulta = new URLSearchParams({ plataforma: 'web' })
  if (filtros.dificultad) {
    consulta.set('dificultad', filtros.dificultad)
  }
  if (filtros.estado) {
    consulta.set('estado', filtros.estado)
  }
  const respuesta = await pedirConSesion(`/api/cursos/${cursoId}/problemas?${consulta}`, { signal })
  if (!respuesta.ok) {
    return { ok: false, status: respuesta.status }
  }
  return { ok: true, problemas: await respuesta.json() }
}

// Mismos campos que el record ProblemaDetalle de user-api (GET /api/problemas/{id}). casos_prueba son solo los
// públicos: admin-api nunca manda los ocultos.
export type CasoDeEjemplo = {
  entrada: string
  salida_esperada: string
}

export type ProblemaDetalle = {
  id: number
  curso_id: number
  titulo: string
  enunciado: string
  lenguaje: string
  dificultad: string
  plataforma: string
  tipo_ejercicio: string
  casos_prueba: CasoDeEjemplo[]
}

export type RespuestaProblema = { ok: true; problema: ProblemaDetalle } | { ok: false; status: number }

export async function obtenerProblema(id: number): Promise<RespuestaProblema> {
  const respuesta = await pedirConSesion(`/api/problemas/${id}`)
  if (!respuesta.ok) {
    return { ok: false, status: respuesta.status }
  }
  return { ok: true, problema: await respuesta.json() }
}
