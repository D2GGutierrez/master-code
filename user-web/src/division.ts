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

// HU33-T4: una sola posición para todos los problemas. Es solo de interfaz: no se borra al cerrar sesión.
export const CLAVE_DIVISION = 'mc_division_paneles'

type Almacen = Pick<Storage, 'getItem' | 'setItem'>

// window.localStorage va dentro del try: en algunos navegadores (almacenamiento bloqueado) solo acceder ya lanza.
export function leerDivisionGuardada(almacen?: Almacen): number {
  try {
    const guardado = (almacen ?? window.localStorage).getItem(CLAVE_DIVISION)
    // Number('') es 0: sin este chequeo, un valor vacío llevaría el divisor al mínimo.
    if (guardado === null || !guardado.trim()) {
      return DIVISION_POR_DEFECTO
    }
    return limitarDivision(Number(guardado))
  } catch {
    return DIVISION_POR_DEFECTO
  }
}

export function guardarDivision(porcentaje: number, almacen?: Almacen): void {
  try {
    (almacen ?? window.localStorage).setItem(CLAVE_DIVISION, String(limitarDivision(porcentaje)))
  } catch {
    // Sin almacenamiento el editor funciona igual; solo no recuerda la posición.
  }
}
