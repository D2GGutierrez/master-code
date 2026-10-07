import { peticion } from './cliente.js'

// peticion agrega el token y lo renueva si vence; status permite distinguir la sesión expirada (401).
export async function listarCursos() {
  const respuesta = await peticion('/api/cursos/')
  // Si el cuerpo no es JSON (p. ej. una página de error), se devuelve vacío en vez de fallar.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}
