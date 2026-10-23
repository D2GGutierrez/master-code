import { pedirConSesion } from './cliente.ts'

// Mismos campos que EjecucionController de user-api (POST /api/ejecuciones). No crea un envío oficial.
export type SolicitudEjecucion = {
  codigo: string
  lenguaje: string
  entrada: string
}

// error_servicio no llega aquí: user-api lo convierte en 503.
export type ResultadoEjecucion = {
  estado: 'ok' | 'error_ejecucion' | 'error_compilacion' | 'tiempo_agotado'
  salida: string
  error: string
  tiempo_ms: number | null
}

export type RespuestaEjecucion =
  | { ok: true; resultado: ResultadoEjecucion }
  | { ok: false; status: number; detail?: string }

export async function ejecutarCodigo(solicitud: SolicitudEjecucion): Promise<RespuestaEjecucion> {
  const respuesta = await pedirConSesion('/api/ejecuciones', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(solicitud),
  })
  if (respuesta.ok) {
    return { ok: true, resultado: await respuesta.json() }
  }
  // Los errores de user-api llegan como { detail }; si el cuerpo no es JSON, sin mensaje.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: false, status: respuesta.status, detail: typeof cuerpo.detail === 'string' ? cuerpo.detail : undefined }
}
