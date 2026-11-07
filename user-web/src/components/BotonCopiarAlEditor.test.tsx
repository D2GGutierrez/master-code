import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import BotonCopiarAlEditor from './BotonCopiarAlEditor.tsx'

// El componente no habla con user-api en estas pruebas: se simula guardarBorrador().
const api = vi.hoisted(() => ({ guardarBorrador: vi.fn() }))
vi.mock('../api/borradores.ts', () => api)

const CODIGO = 'def suma(a, b):\n    return a + b\n'

afterEach(() => {
  cleanup()
  api.guardarBorrador.mockReset()
})

function dibujar(codigo = CODIGO) {
  const navegar = vi.fn()
  render(<BotonCopiarAlEditor problemaId={7} codigo={codigo} numeroIntento={3} navegar={navegar} />)
  return navegar
}

// Una promesa que se resuelve o rechaza cuando la prueba lo decide.
function pendiente() {
  let resolver: () => void = () => {}
  let rechazar: (error: Error) => void = () => {}
  const promesa = new Promise<void>((ok, mal) => {
    resolver = ok
    rechazar = mal
  })
  return { promesa, resolver, rechazar }
}

const boton = () => screen.getByRole('button', { name: 'Copiar al editor el código del intento 3' }) as HTMLButtonElement
const confirmacion = () => screen.queryByRole('group', { name: '¿Copiar este código al editor?' })
const cancelar = () => screen.getByRole('button', { name: 'Cancelar' }) as HTMLButtonElement

function abrirYConfirmar() {
  fireEvent.click(boton())
  fireEvent.click(screen.getByRole('button', { name: 'Copiar al editor' }))
}

describe('BotonCopiarAlEditor', () => {
  it.each(['', '   ', '\n\n'])('(a) con código %j no dibuja ningún botón', (codigo) => {
    dibujar(codigo)
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('(b) con código, el botón existe, está habilitado y dice «Copiar al editor actual»', () => {
    dibujar()
    expect(boton().disabled).toBe(false)
    expect(boton().textContent).toBe('Copiar al editor actual')
  })

  it('(c) al pulsarlo pide confirmación sin guardar todavía', () => {
    dibujar()
    fireEvent.click(boton())
    expect(confirmacion()?.textContent).toContain(
      'El código de este envío reemplazará el código que hay ahora en tu editor y tu borrador guardado de este ' +
        'problema. Esta acción no se puede deshacer.',
    )
    expect(api.guardarBorrador).not.toHaveBeenCalled()
  })

  it('(d) «Cancelar» y Escape cierran sin guardar ni navegar', () => {
    const navegar = dibujar()
    fireEvent.click(boton())
    fireEvent.click(cancelar())
    expect(confirmacion()).toBeNull()
    expect(document.activeElement).toBe(boton())

    fireEvent.click(boton())
    fireEvent.keyDown(cancelar(), { key: 'Escape' })
    expect(confirmacion()).toBeNull()
    expect(api.guardarBorrador).not.toHaveBeenCalled()
    expect(navegar).not.toHaveBeenCalled()
  })

  it('(e) «Copiar al editor» guarda una vez el código exacto y después abre el editor', async () => {
    api.guardarBorrador.mockResolvedValue(undefined)
    const navegar = dibujar()
    abrirYConfirmar()
    await act(async () => {})
    expect(api.guardarBorrador).toHaveBeenCalledTimes(1)
    expect(api.guardarBorrador).toHaveBeenCalledWith(7, CODIGO)
    expect(navegar).toHaveBeenCalledTimes(1)
    expect(navegar).toHaveBeenCalledWith('/problemas/7/editor')
    expect(api.guardarBorrador.mock.invocationCallOrder[0]).toBeLessThan(navegar.mock.invocationCallOrder[0])
  })

  it('(f) un doble clic rápido guarda y navega una sola vez', async () => {
    const guardado = pendiente()
    api.guardarBorrador.mockReturnValue(guardado.promesa)
    const navegar = dibujar()
    fireEvent.click(boton())
    const copiar = screen.getByRole('button', { name: 'Copiar al editor' })
    copiar.click()
    copiar.click()
    await act(async () => guardado.resolver())
    expect(api.guardarBorrador).toHaveBeenCalledTimes(1)
    expect(navegar).toHaveBeenCalledTimes(1)
  })

  it('(g) mientras guarda, los dos botones están deshabilitados y dice «Copiando…»', async () => {
    const guardado = pendiente()
    api.guardarBorrador.mockReturnValue(guardado.promesa)
    dibujar()
    abrirYConfirmar()
    const copiando = screen.getByRole('button', { name: 'Copiando…' }) as HTMLButtonElement
    expect(copiando.disabled).toBe(true)
    expect(copiando.getAttribute('aria-busy')).toBe('true')
    expect(cancelar().disabled).toBe(true)
    await act(async () => guardado.resolver())
  })

  it('(h) si falla, sigue abierta con el mensaje, no navega y se puede reintentar', async () => {
    api.guardarBorrador.mockRejectedValueOnce(new Error('400')).mockResolvedValueOnce(undefined)
    const navegar = dibujar()
    abrirYConfirmar()
    expect((await screen.findByRole('alert')).textContent).toBe(
      'No se pudo copiar el código al editor. Inténtalo de nuevo.',
    )
    expect(confirmacion()).not.toBeNull()
    expect(navegar).not.toHaveBeenCalled()
    const copiar = screen.getByRole('button', { name: 'Copiar al editor' }) as HTMLButtonElement
    expect(copiar.disabled).toBe(false)
    expect(cancelar().disabled).toBe(false)

    fireEvent.click(copiar)
    await act(async () => {})
    expect(api.guardarBorrador).toHaveBeenCalledTimes(2)
    expect(navegar).toHaveBeenCalledWith('/problemas/7/editor')
  })

  it('(i) al abrir la confirmación el foco está en «Cancelar»', () => {
    dibujar()
    fireEvent.click(boton())
    expect(document.activeElement).toBe(cancelar())
  })

  it('(j) un código con <script> se guarda tal cual, como texto', async () => {
    api.guardarBorrador.mockResolvedValue(undefined)
    const codigo = '<script>alert(1)</script>'
    dibujar(codigo)
    abrirYConfirmar()
    await act(async () => {})
    expect(api.guardarBorrador).toHaveBeenCalledWith(7, codigo)
    expect(document.querySelector('script')).toBeNull()
  })
})
