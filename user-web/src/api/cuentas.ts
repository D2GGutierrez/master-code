// El navegador corre en la PC, así que localhost:8000 también sirve cuando admin-api está en Docker.
const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8000'

export async function registrarEstudiante(datos: Record<string, string>) {
  const respuesta = await fetch(`${API_URL}/api/auth/registro/estudiante/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  // Si el cuerpo no es JSON (p. ej. una página de error), se devuelve vacío en vez de fallar.
  const cuerpo: Record<string, string[]> = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

// Lo que puede responder admin-api: los tokens si sale bien; detail o errores por campo si no.
export type CuerpoSesion = {
  access?: string
  refresh?: string
  detail?: string
  email?: string[]
  password?: string[]
}

export async function iniciarSesion(datos: Record<string, string>) {
  const respuesta = await fetch(`${API_URL}/api/auth/login/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    // admin-api exige saber desde qué aplicación se inicia sesión (claim cliente).
    body: JSON.stringify({ ...datos, cliente: 'user-web' }),
  })
  const cuerpo: CuerpoSesion = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

// Lo que puede responder admin-api al recuperar: detail (éxito o enlace inválido) o errores por campo.
export type CuerpoRecuperacion = {
  detail?: string
  email?: string[]
  password?: string[]
}

export async function solicitarRecuperacion(correo: string) {
  const respuesta = await fetch(`${API_URL}/api/auth/recuperar/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: correo }),
  })
  const cuerpo: CuerpoRecuperacion = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

// uid y token llegan en el enlace del correo.
export async function restablecerContrasena(datos: { uid: string; token: string; password: string }) {
  const respuesta = await fetch(`${API_URL}/api/auth/nueva-contrasena/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  const cuerpo: CuerpoRecuperacion = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}

export async function renovarTokens(refresh: string) {
  const respuesta = await fetch(`${API_URL}/api/auth/refresh/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refresh }),
  })
  const cuerpo: CuerpoSesion = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}
