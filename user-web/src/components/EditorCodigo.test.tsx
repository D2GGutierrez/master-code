import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import EditorCodigo from './EditorCodigo.tsx'

type PropsMonaco = {
  value?: string
  onChange?: (texto: string | undefined) => void
  language?: string
}

// Últimos props que nuestro componente le pasó a Monaco. Se comparan estos y no textarea.value
// porque jsdom convierte \r\n en \n al leer un textarea, y eso escondería lo que hace el componente.
const monaco = vi.hoisted(() => ({ props: {} as PropsMonaco }))

// Monaco necesita un navegador real: se sustituye por un textarea que muestra lo que recibe.
vi.mock('@monaco-editor/react', () => ({
  default: (props: PropsMonaco) => {
    monaco.props = props
    return (
      <textarea
        aria-label="Monaco simulado"
        data-language={props.language}
        value={props.value}
        onChange={(evento) => props.onChange?.(evento.target.value)}
      />
    )
  },
}))

afterEach(() => {
  cleanup()
  monaco.props = {}
})

function montar(valor: string, lenguaje = 'python') {
  const onCambio = vi.fn()
  const vista = render(<EditorCodigo valor={valor} onCambio={onCambio} lenguaje={lenguaje} />)
  return { onCambio, vista }
}

// Lo que hace Monaco cuando el estudiante escribe: llamar a onChange con el texto completo.
function escribir(texto: string) {
  act(() => monaco.props.onChange?.(texto))
}

// El texto llega intacto a Monaco y vuelve intacto por onCambio.
function comprobarIdaYVuelta(texto: string) {
  const { onCambio } = montar(texto)
  expect(monaco.props.value).toBe(texto)
  escribir(texto)
  expect(onCambio).toHaveBeenCalledTimes(1)
  expect(onCambio).toHaveBeenCalledWith(texto)
}

function contarCodePoints(texto: string) {
  return [...texto].length
}

describe('EditorCodigo con código largo y caracteres especiales', () => {
  it('pasa un código de 5000 líneas sin truncarlo y onCambio devuelve exactamente lo escrito', () => {
    const codigo = Array.from({ length: 5000 }, (_, i) => `valor_${i} = ${i}  # línea ${i + 1}`).join('\n')

    comprobarIdaYVuelta(codigo)
    expect(monaco.props.value?.split('\n')).toHaveLength(5000)
    expect(monaco.props.value).toHaveLength(codigo.length)
  })

  it('no corta ni altera una sola línea de 20000 caracteres', () => {
    const linea = 'a+b;'.repeat(5000)

    comprobarIdaYVuelta(linea)
    expect(monaco.props.value).toHaveLength(20000)
    expect(monaco.props.value).not.toContain('\n')
  })

  it('preserva tildes y ñ, tanto en NFC como en NFD, sin normalizarlas', () => {
    const nfc = 'Año, señal, canción, ÁÉÍÓÚ, ü'
    // Mismo texto en NFD: letra base + acento combinable (U+0301 agudo, U+0303 tilde, U+0308 diéresis).
    const nfd = 'An\u0303o, sen\u0303al, cancio\u0301n, A\u0301E\u0301I\u0301O\u0301U\u0301, u\u0308'
    expect(nfd).not.toBe(nfc)
    expect(nfd.normalize('NFC')).toBe(nfc)

    comprobarIdaYVuelta(nfc)
    cleanup()
    comprobarIdaYVuelta(nfd)
    expect(monaco.props.value).not.toBe(nfc)
  })

  it('preserva emojis completos sin partir pares sustitutos', () => {
    const emojis = 'print("😀 ❤️ 👨‍👩‍👧 🇵🇪")'
    const sustitutoSuelto = /[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]/

    comprobarIdaYVuelta(emojis)
    const recibido = monaco.props.value ?? ''
    // 😀 = 1, ❤️ = 2 (corazón + selector de variante), 👨‍👩‍👧 = 5 (3 personas + 2 ZWJ), 🇵🇪 = 2.
    expect(contarCodePoints('😀')).toBe(1)
    expect(contarCodePoints('❤️')).toBe(2)
    expect(contarCodePoints('👨‍👩‍👧')).toBe(5)
    expect(contarCodePoints('🇵🇪')).toBe(2)
    expect(recibido).toContain('👨‍👩‍👧')
    expect(contarCodePoints(recibido)).toBe(contarCodePoints(emojis))
    expect(recibido).not.toMatch(sustitutoSuelto)
  })

  it('preserva tabulaciones reales sin convertirlas en espacios', () => {
    const codigo = 'def f(x):\n\tif x:\n\t\treturn 1\n\treturn 0'

    comprobarIdaYVuelta(codigo)
    expect(monaco.props.value?.match(/\t/g)).toHaveLength(4)
  })

  it('preserva la sangría mixta de tabulaciones y espacios', () => {
    const codigo = 'def f(x):\n\tif x:\n\t    return 1\n    \treturn 0'

    comprobarIdaYVuelta(codigo)
  })

  // El componente no toca los saltos de línea: \n y \r\n pasan tal cual en ambos sentidos.
  it('preserva los saltos de línea \\n', () => {
    comprobarIdaYVuelta('let a = 1;\nlet b = 2;\n')
  })

  it('preserva los saltos de línea \\r\\n de Windows sin convertirlos en \\n', () => {
    const codigo = 'let a = 1;\r\nlet b = 2;\r\n'

    comprobarIdaYVuelta(codigo)
    expect(monaco.props.value?.match(/\r\n/g)).toHaveLength(2)
  })

  it.each(['python', 'java', 'javascript'])('pasa el lenguaje %s al editor sin cambios', (lenguaje) => {
    montar('x', lenguaje)

    expect(monaco.props.language).toBe(lenguaje)
    expect(screen.getByLabelText('Monaco simulado').getAttribute('data-language')).toBe(lenguaje)
  })

  it('no pierde un contenido largo al cambiar de lenguaje', () => {
    const codigo = Array.from({ length: 3000 }, (_, i) => `// línea ${i + 1} ñ 😀\tfin`).join('\n')
    const { onCambio, vista } = montar(codigo, 'python')

    vista.rerender(<EditorCodigo valor={codigo} onCambio={onCambio} lenguaje="java" />)

    expect(monaco.props.language).toBe('java')
    expect(monaco.props.value).toBe(codigo)
    expect(onCambio).not.toHaveBeenCalled()
  })

  it('preserva el texto vacío', () => {
    comprobarIdaYVuelta('')
  })

  it('preserva un texto con solo espacios', () => {
    comprobarIdaYVuelta('    ')
  })
})
