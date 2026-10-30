import { pedirConSesion } from './cliente.ts'

export type ResultadoEnvio = 'correcto' | 'parcial' | 'incorrecto'

// Un caso oculto trae solo publico y paso (HU39); uno público, además, lo que se ejecutó. error_servicio no llega
// aquí: user-api lo convierte en 503 y no guarda el envío.
export type CasoCalificado = {
  publico: boolean
  paso: boolean
  entrada?: string
  salida_esperada?: string
  salida_obtenida?: string
  error?: string
  estado?: 'ok' | 'error_ejecucion' | 'error_compilacion' | 'tiempo_agotado'
}

// Mismos campos que EnviosController de user-api (POST /api/problemas/{id}/envios, ADR-7 D7).
export type EnvioCalificado = {
  id: number
  numero_intento: number
  resultado: ResultadoEnvio
  puntaje: number
  casos_superados: number
  casos_total: number
  // Mensaje general cuando el código no compiló, se pasó de tiempo o falló al ejecutarse; null si no hubo nada de eso.
  aviso: string | null
  casos: CasoCalificado[]
}

export type RespuestaEnvio =
  | { ok: true; envio: EnvioCalificado }
  | { ok: false; status: number; detail?: string }

// Cada llamada es un intento oficial: no se reintenta sola. El estudiante y la plataforma salen del token.
export async function enviarSolucion(problemaId: number, codigo: string): Promise<RespuestaEnvio> {
  const respuesta = await pedirConSesion(`/api/problemas/${problemaId}/envios`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ codigo }),
  })
  if (respuesta.ok) {
    return { ok: true, envio: await respuesta.json() }
  }
  // Los errores de user-api llegan como { detail }; si el cuerpo no es JSON, sin mensaje.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: false, status: respuesta.status, detail: typeof cuerpo.detail === 'string' ? cuerpo.detail : undefined }
}
