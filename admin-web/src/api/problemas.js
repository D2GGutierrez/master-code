import { peticion } from './cliente.js'

// El curso va en la URL; el docente no se envía: admin-api lo toma del token.
export async function crearProblema(cursoId, datos) {
  const respuesta = await peticion(`/api/cursos/${cursoId}/problemas/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  // Si el cuerpo no es JSON (p. ej. una página de error), se devuelve vacío en vez de fallar.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}
