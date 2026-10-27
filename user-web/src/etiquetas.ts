// Valores que guarda admin-api (DIFICULTADES en problemas/serializers.py y TipoEjercicio en problemas/models.py).
// Map y no objeto: así un valor como "constructor" no encuentra propiedades heredadas.
const ETIQUETA_POR_VALOR = new Map([
  ['facil', 'Fácil'],
  ['medio', 'Medio'],
  ['dificil', 'Difícil'],
  ['escritura_codigo', 'Escritura de código'],
  ['prediccion_salida', 'Predecir la salida'],
  ['opcion_multiple', 'Opción múltiple'],
  ['completar_espacio', 'Completar espacio'],
  // Estado del problema para el estudiante (user-api, EstadoProblema).
  ['resuelto', 'Resuelto'],
  ['intentado', 'Intentado'],
  ['sin_abrir', 'Sin abrir'],
])

// Un valor desconocido se muestra tal cual llegó.
function etiqueta(valor: string): string {
  return ETIQUETA_POR_VALOR.get(valor.trim().toLowerCase()) ?? valor
}

export function etiquetaDificultad(valor: string): string {
  return etiqueta(valor)
}

export function etiquetaTipoEjercicio(valor: string): string {
  return etiqueta(valor)
}

export function etiquetaEstado(valor: string): string {
  return etiqueta(valor)
}
