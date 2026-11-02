import { describe, expect, it } from 'vitest'
import { codigoInicial } from './codigoInicial.ts'

const BASE = 'def suma(a, b):\n    pass\n'

describe('codigoInicial', () => {
  it('(a) sin código base ni borrador empieza vacío', () => {
    expect(codigoInicial(undefined)).toBe('')
  })

  it('(b) null y undefined dan vacío', () => {
    expect(codigoInicial(null)).toBe('')
    expect(codigoInicial(undefined, undefined)).toBe('')
  })

  it('(c) un código base vacío da vacío', () => {
    expect(codigoInicial('')).toBe('')
  })

  it('(d) el código base vuelve idéntico, con sus saltos de línea y su sangría', () => {
    expect(codigoInicial(BASE)).toBe(BASE)
  })

  it('(e) un código base con solo espacios no se recorta', () => {
    expect(codigoInicial('   \n\t')).toBe('   \n\t')
  })

  it('(f) un borrador con texto gana al código base', () => {
    expect(codigoInicial(BASE, 'print(1)')).toBe('print(1)')
  })

  it('(g) un borrador vacío también gana: el estudiante borró todo', () => {
    expect(codigoInicial(BASE, '')).toBe('')
  })

  it('(h) sin borrador (null o undefined) gana el código base', () => {
    expect(codigoInicial(BASE, null)).toBe(BASE)
    expect(codigoInicial(BASE, undefined)).toBe(BASE)
  })

  it('(i) un código base con HTML vuelve idéntico: es texto', () => {
    expect(codigoInicial('<script>alert(1)</script>')).toBe('<script>alert(1)</script>')
  })
})
