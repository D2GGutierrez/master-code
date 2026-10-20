import { renovar } from './sesion.ts'

// El navegador corre en la PC, así que localhost:8081 también sirve cuando user-api está en Docker.
const USER_API_URL = import.meta.env.VITE_USER_API_URL ?? 'http://localhost:8081'

function pedir(ruta: string) {
  return fetch(`${USER_API_URL}${ruta}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('access')}` },
  })
}

// GET a user-api con el access; ante un 401 renueva y reintenta una sola vez.
export async function pedirConSesion(ruta: string) {
  const respuesta = await pedir(ruta)
  if (respuesta.status !== 401) {
    return respuesta
  }
  if (await renovar()) {
    return pedir(ruta)
  }
  // renovar() ya borró los tokens: la sesión expiró del todo.
  window.location.assign('/login')
  throw new Error('Sesión expirada')
}
