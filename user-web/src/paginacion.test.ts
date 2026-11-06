import { describe, expect, it } from 'vitest'
import { leerPagina, paginar } from './paginacion.ts'

const VEINTICINCO = Array.from({ length: 25 }, (_, indice) => indice + 1)

describe('paginar', () => {
  it('(a) reparte 25 elementos en páginas de 10', () => {
    expect(paginar(VEINTICINCO, 1, 10)).toMatchObject({ pagina: 1, totalPaginas: 3, desde: 1, hasta: 10, total: 25 })
    expect(paginar(VEINTICINCO, 1, 10).items).toHaveLength(10)
    expect(paginar(VEINTICINCO, 2, 10)).toMatchObject({ desde: 11, hasta: 20 })
    const ultima = paginar(VEINTICINCO, 3, 10)
    expect(ultima.items).toEqual([21, 22, 23, 24, 25])
    expect(ultima).toMatchObject({ desde: 21, hasta: 25 })
  })

  it('(b) una página fuera de rango se limita a la válida más cercana', () => {
    expect(paginar(VEINTICINCO, 0, 10).pagina).toBe(1)
    expect(paginar(VEINTICINCO, -4, 10).pagina).toBe(1)
    expect(paginar(VEINTICINCO, Number.NaN, 10).pagina).toBe(1)
    expect(paginar(VEINTICINCO, 99, 10).pagina).toBe(3)
  })

  it('(c) una lista vacía es una sola página sin elementos', () => {
    expect(paginar([], 5, 10)).toEqual({ items: [], pagina: 1, totalPaginas: 1, desde: 0, hasta: 0, total: 0 })
  })

  it('(d) 10 elementos son una página; 11 son dos', () => {
    expect(paginar(VEINTICINCO.slice(0, 10), 1, 10).totalPaginas).toBe(1)
    const once = paginar(VEINTICINCO.slice(0, 11), 2, 10)
    expect(once.totalPaginas).toBe(2)
    expect(once.items).toEqual([11])
  })

  it('(f) no modifica la lista original', () => {
    const lista = [3, 2, 1]
    paginar(lista, 1, 2)
    expect(lista).toEqual([3, 2, 1])
  })
})

describe('leerPagina', () => {
  it('(e) solo acepta un entero desde 1', () => {
    expect(leerPagina('?pagina=2')).toBe(2)
    for (const search of ['', '?pagina=', '?pagina=abc', '?pagina=0', '?pagina=-3', '?pagina=2.5', '?pagina=2abc']) {
      expect(leerPagina(search)).toBe(1)
    }
    expect(leerPagina('?otro=1&pagina=3')).toBe(3)
  })
})
