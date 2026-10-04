// Ruta relativa: en desarrollo el proxy de Vite la reenvía a admin-api.
export async function registrarDocente(datos) {
  const respuesta = await fetch('/api/auth/registro/docente/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  // Si el cuerpo no es JSON (p. ej. una página de error), se devuelve vacío en vez de fallar.
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, cuerpo }
}
