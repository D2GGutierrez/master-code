import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ESPERA_MS } from '../useAutoguardado.ts'
import EditorProblema from './EditorProblema.tsx'

// La pantalla no habla con user-api: se simulan el problema y el borrador.
const api = vi.hoisted(() => ({ obtenerProblema: vi.fn(), obtenerBorrador: vi.fn(), guardarBorrador: vi.fn() }))
vi.mock('../api/problemas.ts', () => ({ obtenerProblema: api.obtenerProblema }))
vi.mock('../api/borradores.ts', async (original) => ({
  ...(await original<typeof import('../api/borradores.ts')>()),
  obtenerBorrador: api.obtenerBorrador,
  guardarBorrador: api.guardarBorrador,
}))
vi.mock('../api/ejecucion.ts', () => ({ ejecutarCodigo: vi.fn() }))
vi.mock('../api/envios.ts', () => ({ enviarSolucion: vi.fn(), listarEnvios: vi.fn() }))
// Monaco necesita un navegador real: se sustituye por un textarea, como en EditorCodigo.test.tsx.
vi.mock('@monaco-editor/react', () => ({
  default: (props: { value?: string; onChange?: (texto: string | undefined) => void }) => (
    <textarea aria-label="Monaco simulado" value={props.value} onChange={(e) => props.onChange?.(e.target.value)} />
  ),
}))

const BASE = 'def suma(a, b):\n    pass\n'
const BORRADOR = 'def suma(a, b):\n    return a + b\n'

const PROBLEMA = {
  id: 7,
  curso_id: 1,
  titulo: 'Suma',
  enunciado: 'Suma dos números.',
  lenguaje: 'python',
  dificultad: 'facil',
  plataforma: 'web',
  tipo_ejercicio: 'escritura_codigo',
  codigo_base: BASE,
  casos_prueba: [],
}

async function abrir() {
  render(<EditorProblema id={7} />)
  return (await screen.findByLabelText('Monaco simulado')) as HTMLTextAreaElement
}

beforeEach(() => {
  localStorage.setItem('access', 'token-de-prueba')
  api.obtenerProblema.mockResolvedValue({ ok: true, problema: PROBLEMA })
  api.guardarBorrador.mockResolvedValue(undefined)
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.clearAllMocks()
  localStorage.clear()
})

describe('EditorProblema (HU35-T3)', () => {
  it('con borrador guardado, el editor abre con el borrador exacto', async () => {
    api.obtenerBorrador.mockResolvedValue(BORRADOR)
    expect((await abrir()).value).toBe(BORRADOR)
    expect(api.obtenerBorrador).toHaveBeenCalledWith(7)
  })

  it('sin borrador, el editor abre con el código base', async () => {
    api.obtenerBorrador.mockResolvedValue(null)
    expect((await abrir()).value).toBe(BASE)
  })

  it('si no se puede leer el borrador, no abre el editor y permite reintentar', async () => {
    api.obtenerBorrador.mockRejectedValueOnce(new Error('red'))
    render(<EditorProblema id={7} />)
    expect((await screen.findByRole('alert')).textContent).toBe('No se pudo cargar el problema. Inténtalo de nuevo.')
    expect(screen.queryByLabelText('Monaco simulado')).toBeNull()
    api.obtenerBorrador.mockResolvedValue(BORRADOR)
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(((await screen.findByLabelText('Monaco simulado')) as HTMLTextAreaElement).value).toBe(BORRADOR)
  })

  it('no guarda al abrir; un cambio se guarda como borrador a los 3 s', async () => {
    api.obtenerBorrador.mockResolvedValue(BORRADOR)
    const editor = await abrir()
    vi.useFakeTimers()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(ESPERA_MS * 2)
    })
    expect(api.guardarBorrador).not.toHaveBeenCalled()
    fireEvent.change(editor, { target: { value: 'print(1)\n' } })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(ESPERA_MS)
    })
    expect(api.guardarBorrador).toHaveBeenCalledTimes(1)
    expect(api.guardarBorrador).toHaveBeenCalledWith(7, 'print(1)\n')
  })
})
