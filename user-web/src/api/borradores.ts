import { pedirConSesion } from './cliente.ts'

// Mismos campos que BorradorRepository.Borrador de user-api (GET /api/problemas/{id}/borrador, HU35-T2). Sin borrador
// responde 200 con codigo en null; un borrador vacío ("") es un borrador de verdad (el estudiante borró todo).
type RespuestaBorrador = { codigo: string | null }

// Un fallo con respuesta de user-api: el status decide si vale la pena reintentar (useAutoguardado).
export class ErrorBorrador extends Error {
  status: number

  constructor(status: number) {
    super(`user-api respondió ${status}`)
    this.status = status
  }
}

// El código del borrador, o null si el estudiante todavía no tiene uno en este problema.
export async function obtenerBorrador(problemaId: number): Promise<string | null> {
  const respuesta = await pedirConSesion(`/api/problemas/${problemaId}/borrador`)
  if (!respuesta.ok) {
    throw new ErrorBorrador(respuesta.status)
  }
  const cuerpo: RespuestaBorrador = await respuesta.json()
  return typeof cuerpo.codigo === 'string' ? cuerpo.codigo : null
}

// El código va exacto, sin recortar: los espacios y saltos de línea son parte de lo que escribió.
export async function guardarBorrador(problemaId: number, codigo: string): Promise<void> {
  const respuesta = await pedirConSesion(`/api/problemas/${problemaId}/borrador`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ codigo }),
  })
  if (!respuesta.ok) {
    throw new ErrorBorrador(respuesta.status)
  }
}
