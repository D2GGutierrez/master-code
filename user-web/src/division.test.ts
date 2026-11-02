import { describe, expect, it } from 'vitest'
import {
  CLAVE_DIVISION,
  divisionDesdePuntero,
  guardarDivision,
  leerDivisionGuardada,
  limitarDivision,
  moverDivision,
} from './division.ts'

// Almacenamiento falso: un Map con la misma forma que localStorage (getItem devuelve null si no hay valor).
function almacenFalso(inicial?: string) {
  const datos = new Map<string, string>()
  if (inicial !== undefined) {
    datos.set(CLAVE_DIVISION, inicial)
  }
  return {
    datos,
    getItem: (clave: string) => datos.get(clave) ?? null,
    setItem: (clave: string, valor: string) => {
      datos.set(clave, valor)
    },
  }
}

const fallaAlLeer = {
  getItem: (): string | null => {
    throw new Error('almacenamiento bloqueado')
  },
  setItem: () => {},
}

const fallaAlGuardar = {
  getItem: () => null,
  setItem: () => {
    throw new Error('cuota excedida')
  },
}

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

describe('leerDivisionGuardada', () => {
  it('(a) sin valor guardado da el de por defecto', () => {
    expect(leerDivisionGuardada(almacenFalso())).toBe(45)
  })

  it('(b) lee un número guardado', () => {
    expect(leerDivisionGuardada(almacenFalso('60'))).toBe(60)
  })

  it('(c) un texto que no es número da el de por defecto', () => {
    expect(leerDivisionGuardada(almacenFalso('abc'))).toBe(45)
  })

  it('(d) un texto vacío da el de por defecto', () => {
    expect(leerDivisionGuardada(almacenFalso(''))).toBe(45)
  })

  it('(e) un número fuera de rango queda en el límite', () => {
    expect(leerDivisionGuardada(almacenFalso('5'))).toBe(25)
    expect(leerDivisionGuardada(almacenFalso('200'))).toBe(75)
  })

  it('(f) NaN e Infinity dan el de por defecto', () => {
    expect(leerDivisionGuardada(almacenFalso('NaN'))).toBe(45)
    expect(leerDivisionGuardada(almacenFalso('Infinity'))).toBe(45)
  })

  it('(g) si leer lanza error da el de por defecto y no lanza', () => {
    expect(() => leerDivisionGuardada(fallaAlLeer)).not.toThrow()
    expect(leerDivisionGuardada(fallaAlLeer)).toBe(45)
  })
})

describe('guardarDivision', () => {
  it('(h) guarda el porcentaje como texto y se vuelve a leer igual', () => {
    const almacen = almacenFalso()
    guardarDivision(60, almacen)
    expect(almacen.datos.get(CLAVE_DIVISION)).toBe('60')
    expect(leerDivisionGuardada(almacen)).toBe(60)
  })

  it('(i) guarda lo que se sale ya limitado', () => {
    const almacen = almacenFalso()
    guardarDivision(5, almacen)
    expect(almacen.datos.get(CLAVE_DIVISION)).toBe('25')
    guardarDivision(99, almacen)
    expect(almacen.datos.get(CLAVE_DIVISION)).toBe('75')
  })

  it('(j) si guardar lanza error no lanza', () => {
    expect(() => guardarDivision(60, fallaAlGuardar)).not.toThrow()
  })
})
