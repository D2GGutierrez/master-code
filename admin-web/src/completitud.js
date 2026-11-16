// H3: lo que le falta a un ejercicio de opción múltiple, predicción de salida o completar el espacio para poder
// calificarse. Es la misma regla que admin-api (problemas/completitud.py): el servidor guarda como borrador sin aprobar
// un problema incompleto y el formulario no tiene otra pantalla para completarlo, así que se avisa antes de enviar.
// Devuelve { campo, mensaje } con lo primero que falta (campo es el nombre del campo del formulario) o null.
export const MIN_ALTERNATIVAS = 2
export const MAX_ALTERNATIVAS = 6

// HU47-T1: el espacio en blanco es una racha de tres o más guiones bajos dentro del enunciado.
const HUECO = /_{3,}/g

export function contarHuecos(enunciado) {
  return (String(enunciado ?? '').match(HUECO) ?? []).length
}

export function faltanteDelEjercicio({ tipo, enunciado, opciones, respuestaEsperada }) {
  if (tipo === 'opcion_multiple') {
    const lista = opciones ?? []
    if (lista.length < MIN_ALTERNATIVAS || lista.length > MAX_ALTERNATIVAS) {
      return { campo: 'opciones', mensaje: `Agrega entre ${MIN_ALTERNATIVAS} y ${MAX_ALTERNATIVAS} alternativas.` }
    }
    if (lista.some((opcion) => !String(opcion.texto ?? '').trim())) {
      return { campo: 'opciones', mensaje: 'Ninguna alternativa puede estar vacía.' }
    }
    if (lista.filter((opcion) => opcion.es_correcta).length !== 1) {
      return { campo: 'opciones', mensaje: 'Marca exactamente una alternativa como correcta.' }
    }
    return null
  }
  if (tipo === 'prediccion_salida' || tipo === 'completar_espacio') {
    if (tipo === 'completar_espacio' && contarHuecos(enunciado) !== 1) {
      return {
        campo: 'enunciado',
        mensaje:
          'El enunciado debe llevar exactamente un espacio en blanco, marcado con ___ (tres guiones bajos o más), dentro del fragmento de código.',
      }
    }
    if (!String(respuestaEsperada ?? '').trim()) {
      return {
        campo: 'respuesta_esperada',
        mensaje: 'Escribe la respuesta esperada: es lo que se compara con lo que responde el estudiante.',
      }
    }
  }
  return null
}
