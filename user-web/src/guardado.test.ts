import { describe, expect, it } from 'vitest'
import { textoGuardado } from './guardado.ts'

// Fechas en hora local, como las crea el hook con new Date().
const AHORA = new Date(2026, 10, 10, 18, 30)
const HOY = new Date(2026, 10, 10, 9, 5)

describe('textoGuardado', () => {
  it('sin actividad ni guardado previo no dice nada', () => {
    expect(textoGuardado('inactivo', null, AHORA)).toBe('')
    expect(textoGuardado('inactivo', undefined, AHORA)).toBe('')
  })

  it('mientras guarda dice «Guardando…»; en la espera de 3 s, «Cambios sin guardar»', () => {
    expect(textoGuardado('guardando', HOY, AHORA)).toBe('Guardando…')
    expect(textoGuardado('pendiente', HOY, AHORA)).toBe('Cambios sin guardar')
  })

  it('guardado hoy: la hora con ceros, en 24 h', () => {
    expect(textoGuardado('guardado', HOY, AHORA)).toBe('Guardado a las 09:05')
    expect(textoGuardado('guardado', new Date(2026, 10, 10, 0, 0), AHORA)).toBe('Guardado a las 00:00')
    expect(textoGuardado('guardado', new Date(2026, 10, 10, 23, 59), AHORA)).toBe('Guardado a las 23:59')
  })

  it('guardado otro día: también la fecha', () => {
    expect(textoGuardado('guardado', new Date(2026, 10, 9, 23, 59), AHORA)).toBe('Guardado el 09/11 a las 23:59')
    expect(textoGuardado('guardado', new Date(2025, 10, 10, 9, 5), AHORA)).toBe('Guardado el 10/11 a las 09:05')
  })

  it('volver al código ya guardado (inactivo con fecha) sigue mostrando la hora', () => {
    expect(textoGuardado('inactivo', HOY, AHORA)).toBe('Guardado a las 09:05')
  })

  it('con error, el mismo texto haya o no reintentos', () => {
    expect(textoGuardado('error', null, AHORA)).toBe('No se pudo guardar el borrador')
    expect(textoGuardado('error', HOY, AHORA)).toBe('No se pudo guardar el borrador')
  })

  it('una fecha inválida no muestra «NaN» ni «Invalid Date»', () => {
    expect(textoGuardado('guardado', new Date('no es fecha'), AHORA)).toBe('')
    expect(textoGuardado('guardado', '2026-11-10' as unknown as Date, AHORA)).toBe('')
    expect(textoGuardado('guardado', HOY, new Date('no es fecha'))).toBe('Guardado el 10/11 a las 09:05')
  })

  it('no modifica las fechas que recibe', () => {
    const guardado = new Date(HOY)
    const ahora = new Date(AHORA)
    textoGuardado('guardado', guardado, ahora)
    expect(guardado.getTime()).toBe(HOY.getTime())
    expect(ahora.getTime()).toBe(AHORA.getTime())
  })
})
