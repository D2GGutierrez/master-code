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
