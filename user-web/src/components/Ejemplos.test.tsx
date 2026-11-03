import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import Ejemplos from './Ejemplos.tsx'

afterEach(cleanup)

function dibujar(casos: Parameters<typeof Ejemplos>[0]['casos']) {
  return render(<Ejemplos casos={casos} nivel={3} />).container
}

describe('Ejemplos', () => {
  it('(a) sin casos no dibuja nada', () => {
    expect(dibujar([]).innerHTML).toBe('')
  })

  it('(b) dibuja un bloque por caso, con su entrada y su salida', () => {
    dibujar([
      { entrada: '2 3', salida_esperada: '5' },
      { entrada: '10 20', salida_esperada: '30' },
    ])
    expect(screen.getByRole('heading', { name: 'Ejemplo 1' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Ejemplo 2' })).toBeTruthy()
    expect(screen.getByLabelText('Entrada del ejemplo 2').textContent).toBe('10 20')
    expect(screen.getByLabelText('Salida esperada del ejemplo 2').textContent).toBe('30')
  })

  it('(c) muestra la entrada y la salida con el texto exacto', () => {
    dibujar([{ entrada: '3\n1 2 3', salida_esperada: '6' }])
    expect(screen.getByLabelText('Entrada del ejemplo 1').textContent).toBe('3\n1 2 3')
    expect(screen.getByLabelText('Salida esperada del ejemplo 1').textContent).toBe('6')
  })

  it('(d) conserva la sangría y los espacios finales', () => {
    dibujar([{ entrada: '1', salida_esperada: '  x\n    y  \n' }])
    expect(screen.getByLabelText('Salida esperada del ejemplo 1').textContent).toBe('  x\n    y  \n')
  })

  it('(e) una entrada con HTML se muestra como texto y no crea un script', () => {
    const caja = dibujar([{ entrada: '<script>alert(1)</script>', salida_esperada: 'x' }])
    expect(screen.getByLabelText('Entrada del ejemplo 1').textContent).toBe('<script>alert(1)</script>')
    expect(caja.querySelector('script')).toBeNull()
  })

  it('(f) una entrada o salida vacía muestra su nota en vez de un bloque vacío', () => {
    const caja = dibujar([{ entrada: '', salida_esperada: '' }])
    expect(screen.getByText('Sin entrada')).toBeTruthy()
    expect(screen.getByText('Salida vacía')).toBeTruthy()
    expect(caja.querySelector('pre')).toBeNull()
  })

  it('(g) un caso marcado como no público no se muestra; uno público sí', () => {
    dibujar([
      { entrada: 'oculto', salida_esperada: 'secreto', es_publico: false },
      { entrada: 'visible', salida_esperada: 'ok', es_publico: true },
    ])
    expect(screen.queryByText('oculto')).toBeNull()
    expect(screen.queryByText('secreto')).toBeNull()
    expect(screen.getByLabelText('Entrada del ejemplo 1').textContent).toBe('visible')
    expect(screen.queryByRole('heading', { name: 'Ejemplo 2' })).toBeNull()
  })

  it('(h) un caso sin la marca se muestra', () => {
    dibujar([{ entrada: 'sin marca', salida_esperada: 'ok' }])
    expect(screen.getByLabelText('Entrada del ejemplo 1').textContent).toBe('sin marca')
  })

  it('(i) cada bloque se puede enfocar con el teclado y tiene su nombre', () => {
    const caja = dibujar([{ entrada: '1', salida_esperada: '2' }])
    const bloques = caja.querySelectorAll('pre')
    expect(bloques).toHaveLength(2)
    for (const bloque of bloques) {
      expect(bloque.tabIndex).toBe(0)
      expect(bloque.getAttribute('aria-label')).toBeTruthy()
    }
  })
})
