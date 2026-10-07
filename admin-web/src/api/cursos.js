import { peticion } from './cliente.js'

// peticion agrega el token y lo renueva si vence; status permite distinguir la sesión expirada (401).
export async function listarCursos() {
  const respuesta = await peticion('/api/cursos/')
  // Si el cuerpo no es JSON (p. ej. una página de error), se devuelve vacío en vez de fallar.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// datos: { nombre, lenguaje }. El docente no se envía: admin-api lo toma del token.
export async function crearCurso(datos) {
  const respuesta = await peticion('/api/cursos/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}
