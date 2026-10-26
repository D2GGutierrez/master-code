import { pedirConSesion } from './cliente.ts'

// Mismos campos que el record MiCurso de user-api (GET /api/mis-cursos), en snake_case.
export type MiCurso = {
  curso_id: number
  nombre: string
  lenguaje: string
  institucion: string
  fecha_inscripcion: string
}

export async function misCursos(): Promise<MiCurso[]> {
  const respuesta = await pedirConSesion('/api/mis-cursos')
  if (!respuesta.ok) {
    throw new Error(`user-api respondió ${respuesta.status}`)
  }
  return respuesta.json()
}

// Mismos campos que el record CursoDisponible de user-api (GET /api/cursos): los cursos publicados de la
// institución del token. No trae docente ni si el estudiante ya está inscrito.
export type CursoDisponible = {
  curso_id: number
  nombre: string
  lenguaje: string
  institucion: string
}

// La búsqueda la hace user-api (?buscar=, parte del nombre, sin importar mayúsculas). Sin texto, todos.
export async function buscarCursos(texto?: string): Promise<CursoDisponible[]> {
  // URLSearchParams codifica el texto para que «C++» o «a&b» lleguen tal cual.
  const consulta = texto ? `?${new URLSearchParams({ buscar: texto })}` : ''
  const respuesta = await pedirConSesion(`/api/cursos${consulta}`)
  if (!respuesta.ok) {
    throw new Error(`user-api respondió ${respuesta.status}`)
  }
  return respuesta.json()
}
