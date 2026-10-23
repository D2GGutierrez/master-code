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
