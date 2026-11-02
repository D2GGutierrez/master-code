import { describe, expect, it } from 'vitest'
import { numeroDeIntento } from './intento.ts'

describe('numeroDeIntento', () => {
  it('(a) sin envíos previos es el intento 1', () => {
    expect(numeroDeIntento([])).toBe(1)
  })

  it('(b) con tres envíos previos es el 4', () => {
    expect(numeroDeIntento([{ id: 3 }, { id: 2 }, { id: 1 }])).toBe(4)
  })

  it('(c) con un envío previo es el 2', () => {
    expect(numeroDeIntento([{ id: 1 }])).toBe(2)
  })

  it('(d) null y undefined no dan número', () => {
    expect(numeroDeIntento(null)).toBeNull()
    expect(numeroDeIntento(undefined)).toBeNull()
  })

  it('(e) un objeto o un texto no dan número', () => {
    expect(numeroDeIntento({ total: 3 })).toBeNull()
    expect(numeroDeIntento('3')).toBeNull()
  })

  it('(f) con 100 envíos previos es el 101', () => {
    expect(numeroDeIntento(Array.from({ length: 100 }, (_, indice) => ({ id: indice })))).toBe(101)
  })
})
