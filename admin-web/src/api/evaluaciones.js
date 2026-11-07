import { peticion } from './cliente.js'

// HU61-T2, HU62 y HU63-T2: datos es { fecha_inicio, fecha_fin, limite_minutos, problemas } con las fechas en ISO y
// problemas como [{ problema: id, puntaje: número }] en el orden del banco (armarProblemas). El curso va en la ruta;
// admin-api comprueba que es del docente (uno ajeno da 404).
export async function crearEvaluacion(cursoId, datos) {
  const respuesta = await peticion(`/api/cursos/${cursoId}/evaluaciones/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}
