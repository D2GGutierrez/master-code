import { peticion } from './cliente.js'

// HU61-T2 y HU62: datos es { fecha_inicio, fecha_fin, limite_minutos, problemas } con las fechas en ISO y los ids en el
// orden del banco. El curso va en la ruta; admin-api comprueba que es del docente (uno ajeno da 404).
export async function crearEvaluacion(cursoId, datos) {
  const respuesta = await peticion(`/api/cursos/${cursoId}/evaluaciones/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}
