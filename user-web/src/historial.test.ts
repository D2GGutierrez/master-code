import { describe, expect, it } from 'vitest'
import { formatearFecha, formatearPuntaje, numeroDeFila } from './historial.ts'

describe('numeroDeFila', () => {
  it('(a) usa el número que trae el servidor', () => {
    expect(numeroDeFila(5, 0, 9)).toBe(5)
  })

  it('(b) sin número del servidor, lo calcula desde el más reciente', () => {
    expect(numeroDeFila(null, 0, 3)).toBe(3)
    expect(numeroDeFila(null, 2, 3)).toBe(1)
  })

  it('(c) un número del servidor que no sirve cae a total - indice', () => {
    expect(numeroDeFila(0, 1, 4)).toBe(3)
    expect(numeroDeFila(2.5, 1, 4)).toBe(3)
    expect(numeroDeFila(-1, 1, 4)).toBe(3)
    expect(numeroDeFila(NaN, 1, 4)).toBe(3)
  })
})

describe('formatearFecha', () => {
  it('(d) una fecha ISO válida se muestra; lo demás es «—»', () => {
    expect(formatearFecha('2026-11-07T15:30:00Z')).toContain('2026')
    expect(formatearFecha(null)).toBe('—')
    expect(formatearFecha(undefined)).toBe('—')
    expect(formatearFecha('')).toBe('—')
    expect(formatearFecha('no es fecha')).toBe('—')
  })
})

describe('formatearPuntaje', () => {
  it('(e) muestra el puntaje sobre 100', () => {
    expect(formatearPuntaje(100)).toContain('100')
    expect(formatearPuntaje(100)).toContain('/ 100')
    expect(formatearPuntaje(0)).toContain('0')
    expect(formatearPuntaje(66.666)).toContain('66')
  })

  it('(f) sin un número válido es «—»', () => {
    expect(formatearPuntaje(null)).toBe('—')
    expect(formatearPuntaje(undefined)).toBe('—')
    expect(formatearPuntaje(NaN)).toBe('—')
  })
})
