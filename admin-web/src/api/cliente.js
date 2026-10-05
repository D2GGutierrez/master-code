import { borrarTokens, guardarTokens, leerAccess, leerRefresh } from './sesion.js'

// Ruta relativa: en desarrollo el proxy de Vite la reenvía a admin-api.
export const API_URL = '/api/auth'

// Renovación en curso, compartida: el refresh rota, así que si dos peticiones renovaran
// a la vez, la segunda usaría un refresh ya invalidado y se perdería la sesión.
let renovacionEnCurso = null

async function renovar() {
  const refresh = leerRefresh()
  if (refresh) {
    const respuesta = await fetch(`${API_URL}/refresh/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh }),
    })
    if (respuesta.ok) {
      const tokens = await respuesta.json()
      guardarTokens(tokens.access, tokens.refresh)
      return true
    }
  }
  borrarTokens()
  return false
}

function conToken(opciones) {
  const access = leerAccess()
  if (!access) {
    return opciones
  }
  return { ...opciones, headers: { ...opciones.headers, Authorization: `Bearer ${access}` } }
}

export async function peticion(ruta, opciones = {}) {
  const respuesta = await fetch(ruta, conToken(opciones))
  if (respuesta.status !== 401) {
    return respuesta
  }

  if (!renovacionEnCurso) {
    renovacionEnCurso = renovar().finally(() => {
      renovacionEnCurso = null
    })
  }
  if (!(await renovacionEnCurso)) {
    return respuesta
  }
  // Un solo reintento: si vuelve a dar 401 se devuelve tal cual, sin renovar en bucle.
  return fetch(ruta, conToken(opciones))
}
