import { API_URL } from './cliente.js'
import { guardarTokens } from './sesion.js'

export async function registrarDocente(datos) {
  const respuesta = await fetch(`${API_URL}/registro/docente/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  // Si el cuerpo no es JSON (p. ej. una página de error), se devuelve vacío en vez de fallar.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

// Usa fetch y no peticion: un 401 aquí son credenciales incorrectas, no un token vencido.
export async function iniciarSesion(datos) {
  const respuesta = await fetch(`${API_URL}/login/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    // admin-api exige saber desde qué aplicación se inicia sesión (claim cliente).
    body: JSON.stringify({ ...datos, cliente: 'admin-web' }),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  if (respuesta.ok) {
    guardarTokens(cuerpo.access, cuerpo.refresh)
  }
  return { ok: respuesta.ok, cuerpo }
}

// Usa fetch y no peticion: con el access vencido, peticion renovaría (rotando este mismo refresh)
// o, si falla, llevaría al login guardando una ruta, y aquí solo se quiere revocar.
export async function cerrarSesionEnServidor(refresh) {
  const respuesta = await fetch(`${API_URL}/logout/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refresh }),
  })
  // admin-api responde 204 sin cuerpo: queda vacío en vez de fallar.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

export async function solicitarRecuperacion(correo) {
  const respuesta = await fetch(`${API_URL}/recuperar/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: correo }),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

// datos: { uid, token, password }; uid y token llegan en el enlace del correo.
export async function restablecerContrasena(datos) {
  const respuesta = await fetch(`${API_URL}/nueva-contrasena/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}
