// HU33-T1: ancho del panel izquierdo de DosPaneles, en porcentaje del contenedor.
export const DIVISION_MINIMA = 25
export const DIVISION_MAXIMA = 75
export const DIVISION_POR_DEFECTO = 45
const PASO_TECLADO = 2

export function limitarDivision(valor: number): number {
  if (!Number.isFinite(valor)) {
    return DIVISION_POR_DEFECTO
  }
  return Math.min(DIVISION_MAXIMA, Math.max(DIVISION_MINIMA, valor))
}

// clienteX es la posición del puntero; izquierda y ancho, los del contenedor (getBoundingClientRect).
export function divisionDesdePuntero(clienteX: number, izquierda: number, ancho: number): number {
  if (!(ancho > 0)) {
    return DIVISION_POR_DEFECTO
  }
  return limitarDivision(((clienteX - izquierda) / ancho) * 100)
}

export function moverDivision(actual: number, tecla: string): number {
  if (tecla === 'ArrowLeft') {
    return limitarDivision(actual - PASO_TECLADO)
  }
  if (tecla === 'ArrowRight') {
    return limitarDivision(actual + PASO_TECLADO)
  }
  if (tecla === 'Home') {
    return DIVISION_MINIMA
  }
  if (tecla === 'End') {
    return DIVISION_MAXIMA
  }
  return actual
}
