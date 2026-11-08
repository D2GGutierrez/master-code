import type { EstadoAutoguardado } from './useAutoguardado.ts'

function dosCifras(numero: number) {
  return String(numero).padStart(2, '0')
}

function esFechaValida(fecha: unknown): fecha is Date {
  return fecha instanceof Date && !Number.isNaN(fecha.getTime())
}

function mismoDia(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

// HU35-T4: el texto del indicador de guardado, en hora local y 24 h. 'pendiente' es la espera de 3 s antes de guardar:
// todavía no se está guardando. 'error' es el mismo texto se vaya a reintentar o no: el hook no lo expone, y si
// reintenta, el estado pasa a 'guardando'. Vacío si no hay nada que decir o la fecha no sirve.
export function textoGuardado(
  estado: EstadoAutoguardado,
  ultimoGuardado: Date | null | undefined,
  ahora: Date,
): string {
  if (estado === 'guardando') {
    return 'Guardando…'
  }
  if (estado === 'pendiente') {
    return 'Cambios sin guardar'
  }
  if (estado === 'error') {
    return 'No se pudo guardar el borrador'
  }
  if (!esFechaValida(ultimoGuardado)) {
    return ''
  }
  const hora = `${dosCifras(ultimoGuardado.getHours())}:${dosCifras(ultimoGuardado.getMinutes())}`
  if (esFechaValida(ahora) && mismoDia(ultimoGuardado, ahora)) {
    return `Guardado a las ${hora}`
  }
  return `Guardado el ${dosCifras(ultimoGuardado.getDate())}/${dosCifras(ultimoGuardado.getMonth() + 1)} a las ${hora}`
}
