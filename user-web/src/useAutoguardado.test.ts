import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerBorrador } from './api/borradores.ts'
import { ESPERA_MS, MAX_REINTENTOS, REINTENTO_MS, useAutoguardado } from './useAutoguardado.ts'

// Se simula el cliente autenticado: guardarBorrador y obtenerBorrador corren de verdad sobre él.
const cliente = vi.hoisted(() => ({ pedirConSesion: vi.fn() }))
vi.mock('./api/cliente.ts', () => cliente)

function respuesta(status: number, cuerpo: unknown = {}) {
  return { ok: status >= 200 && status < 300, status, json: async () => cuerpo }
}

// Los códigos que llegaron a user-api, en orden.
function guardados(): string[] {
  return cliente.pedirConSesion.mock.calls.map(([, opciones]) => JSON.parse(opciones.body).codigo)
}

const INICIAL = 'def suma(a, b):\n    pass\n'

function montar(habilitado = true) {
  return renderHook(({ codigo }) => useAutoguardado(7, codigo, INICIAL, habilitado), {
    initialProps: { codigo: INICIAL },
  })
}

async function avanzar(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

beforeEach(() => {
  vi.useFakeTimers()
  cliente.pedirConSesion.mockResolvedValue(respuesta(200, { fecha_actualizacion: '2026-11-10T10:00:00Z' }))
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  cliente.pedirConSesion.mockReset()
})

describe('useAutoguardado', () => {
  it('(a) con el código igual al inicial no guarda nunca', async () => {
    const { result } = montar()
    await avanzar(60_000)
    expect(cliente.pedirConSesion).not.toHaveBeenCalled()
    expect(result.current.estado).toBe('inactivo')
  })

  it('(b) guarda a los 3000 ms de la última tecla, no a los 2999, y pasa por pendiente, guardando y guardado', async () => {
    let terminar: (valor: unknown) => void = () => {}
    cliente.pedirConSesion.mockReturnValueOnce(new Promise((resolver) => (terminar = resolver)))
    const { result, rerender } = montar()

    rerender({ codigo: 'x = 1' })
    expect(result.current.estado).toBe('pendiente')
    await avanzar(ESPERA_MS - 1)
    expect(cliente.pedirConSesion).not.toHaveBeenCalled()
    await avanzar(1)
    expect(guardados()).toEqual(['x = 1'])
    expect(cliente.pedirConSesion.mock.calls[0][0]).toBe('/api/problemas/7/borrador')
    expect(result.current.estado).toBe('guardando')

    await act(async () => terminar(respuesta(200, {})))
    expect(result.current.estado).toBe('guardado')
    expect(result.current.ultimoGuardado).toBeInstanceOf(Date)
  })

  it('(c) cinco cambios separados por 1 s dan un solo guardado, 3 s después del último, con el último código', async () => {
    const { rerender } = montar()
    for (const codigo of ['a', 'ab', 'abc', 'abcd', 'abcde']) {
      rerender({ codigo })
      await avanzar(1000)
    }
    expect(cliente.pedirConSesion).not.toHaveBeenCalled()
    await avanzar(ESPERA_MS - 1000)
    expect(guardados()).toEqual(['abcde'])
    await avanzar(30_000)
    expect(guardados()).toEqual(['abcde'])
  })

  it('(d) un cambio durante un guardado no lanza otro a la vez; al terminar se guarda el último', async () => {
    let terminar: (valor: unknown) => void = () => {}
    cliente.pedirConSesion.mockReturnValueOnce(new Promise((resolver) => (terminar = resolver)))
    const { result, rerender } = montar()

    rerender({ codigo: 'v1' })
    await avanzar(ESPERA_MS)
    rerender({ codigo: 'v2' })
    await avanzar(ESPERA_MS * 3)
    expect(guardados()).toEqual(['v1'])

    await act(async () => terminar(respuesta(200, {})))
    expect(result.current.estado).toBe('pendiente')
    await avanzar(ESPERA_MS)
    expect(guardados()).toEqual(['v1', 'v2'])
  })

  it('(e) una respuesta lenta del primer guardado no pisa al segundo', async () => {
    let terminar: (valor: unknown) => void = () => {}
    cliente.pedirConSesion.mockReturnValueOnce(new Promise((resolver) => (terminar = resolver)))
    const { result, rerender } = montar()

    rerender({ codigo: 'v1' })
    await avanzar(ESPERA_MS)
    rerender({ codigo: 'v2' })
    await avanzar(20_000)
    await act(async () => terminar(respuesta(200, {})))
    await avanzar(ESPERA_MS)
    expect(result.current.estado).toBe('guardado')
    // El último que llegó al servidor es el más nuevo, y volver a v2 ya no es un cambio.
    expect(guardados().at(-1)).toBe('v2')
    rerender({ codigo: 'v2' })
    await avanzar(30_000)
    expect(guardados()).toEqual(['v1', 'v2'])
  })

  it('(f) un error de red pasa a error y se reintenta cada 10 s, como máximo 3 veces seguidas', async () => {
    cliente.pedirConSesion.mockRejectedValue(new TypeError('Failed to fetch'))
    const { result, rerender } = montar()

    rerender({ codigo: 'x' })
    await avanzar(ESPERA_MS)
    expect(result.current.estado).toBe('error')
    expect(cliente.pedirConSesion).toHaveBeenCalledTimes(1)
    for (let reintento = 1; reintento <= MAX_REINTENTOS; reintento++) {
      await avanzar(REINTENTO_MS - 1)
      expect(cliente.pedirConSesion).toHaveBeenCalledTimes(reintento)
      await avanzar(1)
      expect(cliente.pedirConSesion).toHaveBeenCalledTimes(reintento + 1)
    }
    await avanzar(REINTENTO_MS * 5)
    expect(cliente.pedirConSesion).toHaveBeenCalledTimes(1 + MAX_REINTENTOS)
    expect(result.current.estado).toBe('error')

    // Un cambio nuevo reinicia el ciclo normal.
    rerender({ codigo: 'xy' })
    await avanzar(ESPERA_MS)
    expect(cliente.pedirConSesion).toHaveBeenCalledTimes(2 + MAX_REINTENTOS)
  })

  it.each([403, 404, 400, 413])('(g) con %i pasa a error y no reintenta', async (status) => {
    cliente.pedirConSesion.mockResolvedValue(respuesta(status, { detail: 'no' }))
    const { result, rerender } = montar()

    rerender({ codigo: 'x' })
    await avanzar(ESPERA_MS)
    await avanzar(REINTENTO_MS * 5)
    expect(result.current.estado).toBe('error')
    expect(cliente.pedirConSesion).toHaveBeenCalledTimes(1)
  })

  it('(h) al desmontar con un cambio pendiente guarda de inmediato; sin cambios, no', async () => {
    const pendiente = montar()
    pendiente.rerender({ codigo: 'sin esperar' })
    pendiente.unmount()
    expect(guardados()).toEqual(['sin esperar'])

    cliente.pedirConSesion.mockClear()
    const sinCambios = montar()
    sinCambios.unmount()
    await avanzar(30_000)
    expect(cliente.pedirConSesion).not.toHaveBeenCalled()
  })

  it('(i) al ocultar la pestaña con un cambio pendiente guarda de inmediato', async () => {
    const { rerender } = montar()
    rerender({ codigo: 'oculta' })
    const visibilidad = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
    await act(async () => {
      document.dispatchEvent(new Event('visibilitychange'))
    })
    expect(guardados()).toEqual(['oculta'])
    await avanzar(30_000)
    expect(guardados()).toEqual(['oculta'])
    visibilidad.mockRestore()
  })

  it('(j) deshabilitado no programa nada', async () => {
    const { result, rerender } = montar(false)
    rerender({ codigo: 'x' })
    await avanzar(60_000)
    expect(cliente.pedirConSesion).not.toHaveBeenCalled()
    expect(result.current.estado).toBe('inactivo')
  })

  it('(k) volver al texto anterior tras guardar es un cambio; volver a lo último guardado no', async () => {
    const { result, rerender } = montar()
    rerender({ codigo: 'nuevo' })
    await avanzar(ESPERA_MS)
    expect(guardados()).toEqual(['nuevo'])

    rerender({ codigo: INICIAL })
    await avanzar(ESPERA_MS)
    expect(guardados()).toEqual(['nuevo', INICIAL])

    rerender({ codigo: 'otro' })
    rerender({ codigo: INICIAL })
    await avanzar(30_000)
    expect(guardados()).toEqual(['nuevo', INICIAL])
    expect(result.current.estado).toBe('guardado')
  })
})

describe('obtenerBorrador', () => {
  it('devuelve el código guardado, exacto, y un borrador vacío como vacío', async () => {
    cliente.pedirConSesion.mockResolvedValueOnce(respuesta(200, { codigo: INICIAL, fecha_actualizacion: '2026-11-10T10:00:00Z' }))
    expect(await obtenerBorrador(7)).toBe(INICIAL)
    expect(cliente.pedirConSesion).toHaveBeenCalledWith('/api/problemas/7/borrador')

    cliente.pedirConSesion.mockResolvedValueOnce(respuesta(200, { codigo: '', fecha_actualizacion: '2026-11-10T10:00:00Z' }))
    expect(await obtenerBorrador(7)).toBe('')
  })

  it('devuelve null cuando el servidor dice que no hay borrador', async () => {
    cliente.pedirConSesion.mockResolvedValueOnce(respuesta(200, { codigo: null, fecha_actualizacion: null }))
    expect(await obtenerBorrador(7)).toBeNull()
  })

  it('un fallo del servidor lanza un error (la pantalla no abre el editor)', async () => {
    cliente.pedirConSesion.mockResolvedValueOnce(respuesta(503))
    await expect(obtenerBorrador(7)).rejects.toThrow()
  })
})
