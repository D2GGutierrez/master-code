import { describe, expect, it } from 'vitest'
import { divisionDesdePuntero, limitarDivision, moverDivision } from './division.ts'

describe('limitarDivision', () => {
  it('(a) lleva lo que se sale a los límites', () => {
    expect(limitarDivision(10)).toBe(25)
    expect(limitarDivision(90)).toBe(75)
  })

  it('(b) deja igual un valor dentro de los límites', () => {
    expect(limitarDivision(50)).toBe(50)
  })

  it('(c) un valor no finito vuelve al de por defecto', () => {
    expect(limitarDivision(NaN)).toBe(45)
    expect(limitarDivision(Infinity)).toBe(45)
  })
})

describe('divisionDesdePuntero', () => {
  it('(d) calcula el porcentaje respecto al contenedor', () => {
    expect(divisionDesdePuntero(500, 100, 800)).toBe(50)
  })

  it('(e) un puntero fuera del contenedor queda en el límite', () => {
    expect(divisionDesdePuntero(50, 100, 800)).toBe(25)
    expect(divisionDesdePuntero(1000, 100, 800)).toBe(75)
  })

  it('(f) sin ancho vuelve al de por defecto', () => {
    expect(divisionDesdePuntero(500, 100, 0)).toBe(45)
  })
})

describe('moverDivision', () => {
  it('(g) las flechas mueven de 2 en 2', () => {
    expect(moverDivision(45, 'ArrowRight')).toBe(47)
    expect(moverDivision(45, 'ArrowLeft')).toBe(43)
  })

  it('(h) las flechas no pasan de los límites', () => {
    expect(moverDivision(74, 'ArrowRight')).toBe(75)
    expect(moverDivision(26, 'ArrowLeft')).toBe(25)
  })

  it('(i) Home va al mínimo y End al máximo', () => {
    expect(moverDivision(45, 'Home')).toBe(25)
    expect(moverDivision(45, 'End')).toBe(75)
  })

  it('(j) otra tecla no cambia nada', () => {
    expect(moverDivision(45, 'a')).toBe(45)
    expect(moverDivision(45, 'Enter')).toBe(45)
  })
})
