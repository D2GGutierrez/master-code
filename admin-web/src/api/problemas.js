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

// Más que los 100 s que admin-api espera a ai-service (AI_SERVICE_TIMEOUT_SEGUNDOS): normalmente corta el servidor.
const TIEMPO_MAXIMO_GENERACION_MS = 120000

// datos: { tema, nivel, lenguaje, cantidad }. Al agotarse el tiempo, fetch lanza un error TimeoutError.
export async function generarProblemas(cursoId, datos) {
  const respuesta = await peticion(`/api/cursos/${cursoId}/problemas/generar/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
    signal: AbortSignal.timeout(TIEMPO_MAXIMO_GENERACION_MS),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// Lista completa, sin paginar: lo generado por IA que el docente aún no aprueba ni descarta, de sus cursos.
export async function listarPendientes() {
  const respuesta = await peticion('/api/problemas/pendientes/')
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// Archiva el problema, no lo borra. admin-api exige confirmar=true para que ninguna llamada suelta descarte
// por accidente: este envío solo ocurre tras el cuadro de confirmación de la bandeja (HU20-T1).
export async function descartarProblema(id) {
  const respuesta = await peticion(`/api/problemas/${id}/descartar/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ confirmar: true }),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// HU62-T2: el banco del curso para armar una evaluación. Lista completa, sin paginar, de los problemas no descartados:
// [{ id, titulo, dificultad, tipo_ejercicio, plataforma, aprobado_por_docente }]. Un curso ajeno da 404.
export async function listarProblemasCurso(cursoId, signal) {
  const respuesta = await peticion(`/api/cursos/${cursoId}/problemas/`, { signal })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// HU19-T1: un problema completo para revisarlo (con casos, alternativas y respuesta esperada). Uno ajeno da 404.
export async function obtenerProblema(id) {
  const respuesta = await peticion(`/api/problemas/${id}/`)
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// PUT: reemplazo completo. Los casos y las alternativas enviados sustituyen a los anteriores.
export async function actualizarProblema(id, datos) {
  const respuesta = await peticion(`/api/problemas/${id}/`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos),
  })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}

// HU19-T2: deja el problema listo para los estudiantes y registra quién y cuándo. Uno incompleto da 400 con el campo
// que falta; uno descartado, 409. Aprobar dos veces no es un error.
export async function aprobarProblema(id) {
  const respuesta = await peticion(`/api/problemas/${id}/aprobar/`, { method: 'POST' })
  const cuerpo = await respuesta.json().catch(() => ({}))
  return { ok: respuesta.ok, status: respuesta.status, cuerpo }
}
