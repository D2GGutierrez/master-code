import type { Avance } from './api/avance.ts'
import type { MiCurso } from './api/cursos.ts'
import type { ProblemaDeLista } from './api/problemas.ts'

// Cuentas de progreso a partir de lo que ya entrega user-api: cada problema de la lista de un curso trae su estado
// para este estudiante (resuelto, intentado o sin abrir). Aquí no se inventa nada: todo sale de esos estados.

export type Resumen = {
  total: number
  resueltos: number
  intentados: number
  sinAbrir: number
  // De 0 a 1; un curso sin problemas es 0.
  fraccion: number
}

export function resumir(problemas: ProblemaDeLista[]): Resumen {
  let resueltos = 0
  let intentados = 0
  for (const problema of problemas) {
    if (problema.estado === 'resuelto') {
      resueltos += 1
    } else if (problema.estado === 'intentado') {
      intentados += 1
    }
  }
  const total = problemas.length
  return {
    total,
    resueltos,
    intentados,
    sinAbrir: total - resueltos - intentados,
    fraccion: total === 0 ? 0 : resueltos / total,
  }
}

export type EstadoCurso = 'sin-problemas' | 'nuevo' | 'en-curso' | 'completo'

export function estadoDelCurso(resumen: Resumen): EstadoCurso {
  if (resumen.total === 0) {
    return 'sin-problemas'
  }
  if (resumen.resueltos === resumen.total) {
    return 'completo'
  }
  return resumen.resueltos > 0 || resumen.intentados > 0 ? 'en-curso' : 'nuevo'
}

// Lista de problemas de cada curso; null si esa lista no se pudo cargar.
export type ProblemasPorCurso = Map<number, ProblemaDeLista[] | null>

// Suma de varios cursos: solo cuentan los que sí cargaron.
export function resumirTodos(problemas: ProblemasPorCurso): Resumen {
  const todos: ProblemaDeLista[] = []
  for (const lista of problemas.values()) {
    if (lista) {
      todos.push(...lista)
    }
  }
  return resumir(todos)
}

// Mismos valores que admin-api (DIFICULTADES). Una dificultad desconocida va al final.
const RANGO_DIFICULTAD = new Map([
  ['facil', 1],
  ['medio', 2],
  ['dificil', 3],
])

const TIPO_CON_EDITOR = 'escritura_codigo'

// El problema que toca después: entre los que faltan, primero los que se resuelven con el editor, luego los que ya
// se intentaron (se retoma lo empezado) y luego el más fácil. null si no falta ninguno.
export function siguienteProblema(problemas: ProblemaDeLista[]): ProblemaDeLista | null {
  const faltan = problemas.filter((problema) => problema.estado !== 'resuelto')
  if (faltan.length === 0) {
    return null
  }
  const sinEditor = (problema: ProblemaDeLista) => (problema.tipo_ejercicio === TIPO_CON_EDITOR ? 0 : 1)
  const sinEmpezar = (problema: ProblemaDeLista) => (problema.estado === 'intentado' ? 0 : 1)
  const rango = (problema: ProblemaDeLista) => RANGO_DIFICULTAD.get(problema.dificultad.trim().toLowerCase()) ?? 9
  return [...faltan].sort(
    (a, b) =>
      sinEditor(a) - sinEditor(b) ||
      sinEmpezar(a) - sinEmpezar(b) ||
      rango(a) - rango(b) ||
      a.titulo.localeCompare(b.titulo, 'es'),
  )[0]
}

export type Continuar = {
  curso: MiCurso
  // null si el curso ya está completo o no tiene problemas.
  problema: ProblemaDeLista | null
  // Si el estudiante ya había enviado algo (se «continúa») o todavía no (se «empieza»).
  conActividad: boolean
}

// El curso donde se quedó el estudiante: el del envío más reciente. Sin envíos conocidos, el primer curso que aún
// tenga algo por resolver (o, si no, el primero).
export function elegirContinuar(cursos: MiCurso[], problemas: ProblemasPorCurso, avance: Avance | null): Continuar | null {
  if (cursos.length === 0) {
    return null
  }
  let ultimo: { curso: MiCurso; tiempo: number } | null = null
  for (const envio of avance?.problemas ?? []) {
    const tiempo = Date.parse(envio.ultimo_envio)
    if (Number.isNaN(tiempo) || (ultimo && tiempo <= ultimo.tiempo)) {
      continue
    }
    const curso = cursos.find((candidato) => problemas.get(candidato.curso_id)?.some((problema) => problema.id === envio.problema_id))
    if (curso) {
      ultimo = { curso, tiempo }
    }
  }
  if (ultimo) {
    return { curso: ultimo.curso, problema: siguienteProblema(problemas.get(ultimo.curso.curso_id) ?? []), conActividad: true }
  }
  const curso =
    cursos.find((candidato) => problemas.get(candidato.curso_id)?.some((problema) => problema.estado !== 'resuelto')) ??
    cursos[0]
  return { curso, problema: siguienteProblema(problemas.get(curso.curso_id) ?? []), conActividad: false }
}
