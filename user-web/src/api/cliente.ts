import { renovar } from './sesion.ts'

// El navegador corre en la PC, así que localhost:8081 también sirve cuando user-api está en Docker.
const USER_API_URL = import.meta.env.VITE_USER_API_URL ?? 'http://localhost:8081'

// headers como objeto simple para poder mezclarlo con Authorization.
type Opciones = Omit<RequestInit, 'headers'> & { headers?: Record<string, string> }

function pedir(ruta: string, opciones: Opciones) {
  return fetch(`${USER_API_URL}${ruta}`, {
    ...opciones,
    headers: { ...opciones.headers, Authorization: `Bearer ${localStorage.getItem('access')}` },
  })
}

// Petición a user-api con el access (GET si no se indica otro método); ante un 401 renueva y reintenta una sola vez.
export async function pedirConSesion(ruta: string, opciones: Opciones = {}) {
  const respuesta = await pedir(ruta, opciones)
  if (respuesta.status !== 401) {
    return respuesta
  }
  if (await renovar()) {
    return pedir(ruta, opciones)
  }
  // renovar() ya borró los tokens: la sesión expiró del todo.
  window.location.assign('/login')
  throw new Error('Sesión expirada')
}
