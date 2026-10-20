import { renovarTokens } from './cuentas.ts'

// Se renueva 60 s antes de que venza el access, para no llegar a usarlo vencido.
const MARGEN_MS = 60_000

let temporizador: number | undefined

// Mismas claves que admin-web. localStorage para que la sesión sobreviva a recargar (decisión simple del MVP).
export function guardarSesion(access: string, refresh: string) {
  localStorage.setItem('access', access)
  localStorage.setItem('refresh', refresh)
  programarRenovacion()
}

function borrarSesion() {
  clearTimeout(temporizador)
  localStorage.removeItem('access')
  localStorage.removeItem('refresh')
}

// Momento en que vence el access, en milisegundos: claim exp (en segundos) de la parte central del JWT.
function vencimiento(access: string) {
  // El JWT usa base64url; atob espera base64 normal.
  const parte = access.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(atob(parte)).exp * 1000
}

export function programarRenovacion() {
  // El refresh rota: una segunda renovación en paralelo usaría un refresh ya invalidado
  // y cerraría la sesión. Por eso nunca hay más de un temporizador.
  clearTimeout(temporizador)
  const access = localStorage.getItem('access')
  if (!access) {
    return
  }
  // Si el access ya venció, la espera es negativa y setTimeout renueva de inmediato.
  temporizador = setTimeout(renovar, vencimiento(access) - MARGEN_MS - Date.now())
}

// Devuelve si se renovó: cliente.ts la usa para reintentar tras un 401.
export async function renovar() {
  try {
    const { ok, cuerpo } = await renovarTokens(localStorage.getItem('refresh') ?? '')
    if (ok && cuerpo.access && cuerpo.refresh) {
      // Se guardan los dos: el refresh anterior ya quedó invalidado.
      guardarSesion(cuerpo.access, cuerpo.refresh)
      return true
    }
  } catch {
    // Sin conexión: se trata igual que un rechazo, sin reintentar en bucle.
  }
  borrarSesion()
  return false
}
