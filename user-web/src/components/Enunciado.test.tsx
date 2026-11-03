import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import Enunciado from './Enunciado.tsx'
import PanelEnunciado from './PanelEnunciado.tsx'

afterEach(cleanup)

// El contenido se dibuja con DOMPurify de verdad (enunciado.ts): así se prueba también lo que la sanitización deja.
function dibujar(texto: string) {
  return render(<Enunciado texto={texto} />).container
}

describe('Enunciado', () => {
  it('(a) dibuja títulos y párrafos como encabezados y párrafos reales', () => {
    const caja = dibujar('<h2>Título</h2><p>Texto</p>')
    expect(caja.querySelector('h2')?.textContent).toBe('Título')
    expect(caja.querySelector('.enunciado > p')?.textContent).toBe('Texto')
  })

  it('(b) dibuja una lista real con sus elementos', () => {
    const caja = dibujar('<ul><li>uno</li><li>dos</li></ul>')
    expect(caja.querySelectorAll('ul > li')).toHaveLength(2)
  })

  it('(c) un bloque de código conserva la sangría exacta', () => {
    const caja = dibujar('<pre><code>for i in x:\n    print(i)</code></pre>')
    expect(caja.querySelector('pre > code')?.textContent).toBe('for i in x:\n    print(i)')
  })

  it('(d) el código en línea queda dentro de su párrafo', () => {
    const caja = dibujar('<p>Usa <code>len(x)</code> aquí</p>')
    expect(caja.querySelector('p > code')?.textContent).toBe('len(x)')
  })

  it('(e) no deja ningún script', () => {
    const caja = dibujar('<p>hola</p><script>alert(1)</script>')
    expect(caja.querySelector('script')).toBeNull()
    expect(caja.textContent).toBe('hola')
  })

  it('(f) no deja imágenes (ni su onerror)', () => {
    const caja = dibujar('<p>x</p><img src=x onerror=alert(1)>')
    expect(caja.querySelector('img')).toBeNull()
  })

  it('(g) quita un href javascript:', () => {
    const enlace = dibujar('<a href="javascript:alert(1)">x</a>').querySelector('a')
    expect(enlace?.getAttribute('href') ?? '').not.toMatch(/javascript/i)
  })

  it('(h) un enlace https se abre en otra pestaña sin acceso a esta', () => {
    const enlace = dibujar('<a href="https://ejemplo.com">x</a>').querySelector('a')
    expect(enlace?.getAttribute('href')).toBe('https://ejemplo.com')
    expect(enlace?.getAttribute('target')).toBe('_blank')
    expect(enlace?.getAttribute('rel')).toBe('noopener noreferrer')
  })

  it('(i) el texto plano conserva su salto de línea', () => {
    const parrafo = dibujar('Línea 1\nLínea 2').querySelector('p.enunciado-texto')
    expect(parrafo?.textContent).toBe('Línea 1\nLínea 2')
  })

  it('(j) un < o > sin etiqueta se muestra como texto, no como HTML', () => {
    const caja = dibujar('Si a < b o x > y, imprime 1')
    expect(caja.querySelector('p.enunciado-texto')?.textContent).toBe('Si a < b o x > y, imprime 1')
    expect(caja.querySelector('.enunciado-texto *')).toBeNull()
  })

  it('(k) un texto vacío o solo con espacios no rompe el componente', () => {
    expect(() => dibujar('')).not.toThrow()
    expect(() => dibujar('   \n ')).not.toThrow()
  })
})

describe('PanelEnunciado', () => {
  it('sin enunciado lo dice en vez de dejar el panel en blanco', () => {
    render(<PanelEnunciado problema={{ titulo: 'Suma', dificultad: 'facil', tipo_ejercicio: 'escritura_codigo', enunciado: '  ' }} />)
    expect(screen.getByText('Este problema no tiene enunciado.')).toBeTruthy()
  })
})
