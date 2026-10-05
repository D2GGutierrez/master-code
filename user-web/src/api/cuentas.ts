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
