import { cleanup, render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

afterEach(() => {
  cleanup()
  vi.unstubAllEnvs()
  vi.resetModules()
  delete window.google
  document.head.querySelectorAll('script[src*="accounts.google.com"]').forEach((s) => s.remove())
})

// El ID de cliente se lee al importar el módulo: se importa de nuevo con cada valor.
async function cargarBoton() {
  return (await import('./BotonGoogle.tsx')).default
}

describe('BotonGoogle', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('sin ID de cliente no muestra nada ni descarga el script de Google', async () => {
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', '')
    const BotonGoogle = await cargarBoton()
    const { container } = render(<BotonGoogle alCredencial={() => {}} />)
    expect(container.innerHTML).toBe('')
    expect(document.head.querySelector('script[src*="accounts.google.com"]')).toBeNull()
  })

  it('con ID de cliente pinta el botón de Google y entrega la credencial', async () => {
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', 'cliente-de-prueba.apps.googleusercontent.com')
    const initialize = vi.fn()
    const renderButton = vi.fn()
    window.google = { accounts: { id: { initialize, renderButton } } }
    const alCredencial = vi.fn()
    const BotonGoogle = await cargarBoton()

    const { container } = render(<BotonGoogle alCredencial={alCredencial} texto="signup_with" />)
    await vi.waitFor(() => expect(renderButton).toHaveBeenCalled())

    expect(container.querySelector('.acceso-google')).not.toBeNull()
    expect(initialize.mock.calls[0][0].client_id).toBe('cliente-de-prueba.apps.googleusercontent.com')
    expect(renderButton.mock.calls[0][1]).toMatchObject({ text: 'signup_with', locale: 'es' })
    initialize.mock.calls[0][0].callback({ credential: 'token-firmado' })
    expect(alCredencial).toHaveBeenCalledWith('token-firmado')
  })

  it('ignora la respuesta de Google si no trae credencial', async () => {
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', 'cliente-de-prueba.apps.googleusercontent.com')
    const initialize = vi.fn()
    window.google = { accounts: { id: { initialize, renderButton: vi.fn() } } }
    const alCredencial = vi.fn()
    const BotonGoogle = await cargarBoton()

    render(<BotonGoogle alCredencial={alCredencial} />)
    await vi.waitFor(() => expect(initialize).toHaveBeenCalled())
    initialize.mock.calls[0][0].callback({})

    expect(alCredencial).not.toHaveBeenCalled()
  })
})
