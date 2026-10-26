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

// plataforma=web: user-api deja los de la web y los de ambas; los solo móviles no se pueden resolver aquí.
export async function listarProblemas(cursoId: number): Promise<RespuestaProblemas> {
  const respuesta = await pedirConSesion(`/api/cursos/${cursoId}/problemas?plataforma=web`)
  if (!respuesta.ok) {
    return { ok: false, status: respuesta.status }
  }
  return { ok: true, problemas: await respuesta.json() }
}
