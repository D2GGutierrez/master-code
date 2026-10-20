import { pedirConSesion } from './cliente.ts'

// Mismos campos que el record Avance de user-api (GET /api/avance), en snake_case.
export type ProblemaAvance = {
  problema_id: number
  envios: number
  desde_web: number
  desde_movil: number
  ultimo_envio: string
}

export type Avance = {
  problemas_intentados: number
  total_envios: number
  desde_web: number
  desde_movil: number
  problemas: ProblemaAvance[]
}

export async function obtenerAvance(): Promise<Avance> {
  const respuesta = await pedirConSesion('/api/avance')
  if (!respuesta.ok) {
    throw new Error(`user-api respondió ${respuesta.status}`)
  }
  return respuesta.json()
}
