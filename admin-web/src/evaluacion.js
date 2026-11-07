import { ordenarComoBanco } from './seleccionProblemas.js'

// Lo que da un <input type="datetime-local">: sin zona, así que el navegador lo lee en la hora del equipo.
const FECHA_LOCAL = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,3})?)?$/

// HU61-T3: el instante en ISO UTC (termina en «Z»), así admin-api no lo interpreta en su propia zona; null si el
// valor está vacío o no es una fecha válida.
export function aInstante(valorLocal) {
  if (typeof valorLocal !== 'string' || !FECHA_LOCAL.test(valorLocal)) {
    return null
  }
  const fecha = new Date(valorLocal)
  return Number.isNaN(fecha.getTime()) ? null : fecha.toISOString()
}

// El input numérico da texto: «90» vale, «1.5», «abc», «0» o «-5» no.
function limiteValido(limite) {
  const texto = typeof limite === 'number' ? String(limite) : limite
  return typeof texto === 'string' && /^\d+$/.test(texto.trim()) && Number(texto) >= 1
}

// HU61-T3 (CA-045.1 a CA-045.4, CA-046.3): los errores por campo, vacío si todo está bien. Las mismas reglas que
// admin-api, para no llamarlo con datos que va a rechazar.
export function validarEvaluacion({ inicio, fin, limite, problemas }) {
  const errores = {}
  const instanteInicio = aInstante(inicio)
  const instanteFin = aInstante(fin)
  if (!instanteInicio) {
    errores.inicio = 'Indica la fecha y hora de inicio.'
  }
  if (!instanteFin) {
    errores.fin = 'Indica la fecha y hora de fin.'
  } else if (instanteInicio && new Date(instanteFin) <= new Date(instanteInicio)) {
    errores.fin = 'La fecha de fin debe ser posterior a la de inicio.'
  }
  if (!limiteValido(limite)) {
    errores.limite = 'Indica un límite de tiempo en minutos, entero y mayor que cero.'
  }
  if (!Array.isArray(problemas) || problemas.length === 0) {
    errores.problemas = 'Selecciona al menos un problema.'
  }
  return errores
}

// HU63-T2 (CA-047.1, CA-047.2): los puntos de cada problema, con las reglas de EvaluacionProblema.puntaje en admin-api
// (DecimalField de 5 dígitos y 2 decimales, mínimo 0). El servidor los exige; en el formulario se proponen al marcar.
export const LIMITES_PUNTOS = { minimo: 0, maximo: 999.99, decimales: 2 }
// El mismo valor por defecto del modelo.
export const PUNTOS_POR_DEFECTO = '1'

// El texto de un campo de puntos como número, o null si no sirve. Acepta punto o coma decimal; rechaza signos,
// notación científica, dígitos que no son ASCII y más decimales de los permitidos.
export function normalizarPuntos(texto, limites = LIMITES_PUNTOS) {
  if (typeof texto !== 'string') {
    return null
  }
  const limpio = texto.trim()
  const partes = limpio.match(/^\d+(?:[.,](\d+))?$/)
  if (!partes || (partes[1] ?? '').length > limites.decimales) {
    return null
  }
  const numero = Number(limpio.replace(',', '.'))
  if (!Number.isFinite(numero) || numero < limites.minimo || numero > limites.maximo) {
    return null
  }
  return numero
}

// Errores por id, solo de los problemas seleccionados: lo escrito en uno desmarcado no se envía.
export function validarPuntos(seleccionados, puntos, limites = LIMITES_PUNTOS) {
  const errores = {}
  for (const id of seleccionados ?? []) {
    if (normalizarPuntos(puntos?.[id], limites) === null) {
      errores[id] = 'Indica los puntos: un número de 0 a 999,99, con hasta 2 decimales.'
    }
  }
  return errores
}

// La lista «problemas» del cuerpo: los seleccionados que siguen en el banco, en su orden, sin repetir, con su puntaje.
export function armarProblemas(banco, seleccionados, puntos) {
  return ordenarComoBanco(banco ?? [], seleccionados ?? []).map((id) => ({
    problema: id,
    puntaje: normalizarPuntos(puntos?.[id]),
  }))
}
