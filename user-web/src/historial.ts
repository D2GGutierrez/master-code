// HU41-T2: cómo se muestra cada fila del historial de envíos. Lo que no llega o no sirve se muestra «—».
const SIN_DATO = '—'
const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
const FORMATO_PUNTAJE = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 })

// La lista llega del más reciente al más antiguo: sin número del servidor, la fila «indice» es el intento total - indice.
export function numeroDeFila(numeroServidor: number | null | undefined, indice: number, total: number): number {
  if (typeof numeroServidor === 'number' && Number.isInteger(numeroServidor) && numeroServidor >= 1) {
    return numeroServidor
  }
  return total - indice
}

export function formatearFecha(valor: unknown): string {
  if (typeof valor !== 'string' || !valor) {
    return SIN_DATO
  }
  const fecha = new Date(valor)
  return Number.isNaN(fecha.getTime()) ? SIN_DATO : FORMATO_FECHA.format(fecha)
}

export function formatearPuntaje(valor: unknown): string {
  if (typeof valor !== 'number' || !Number.isFinite(valor)) {
    return SIN_DATO
  }
  return `${FORMATO_PUNTAJE.format(valor)} / 100`
}
