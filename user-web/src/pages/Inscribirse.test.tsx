import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Inscribirse from './Inscribirse.tsx'

// La pantalla no habla con user-api en estas pruebas: se simula lo que responde inscribirse().
const api = vi.hoisted(() => ({ inscribirse: vi.fn() }))
vi.mock('../api/cursos.ts', () => api)

const INSCRITA = { id: 1, curso_id: 7, nombre: 'Algoritmos I', lenguaje: 'python', fecha: '2026-10-29T10:00:00Z' }

beforeEach(() => {
  // Sin sesión la pantalla redirige a /login: se simula que hay una.
  localStorage.setItem('access', 'token-de-prueba')
})

afterEach(() => {
  cleanup()
  localStorage.clear()
  api.inscribirse.mockReset()
})

function escribirYEnviar(codigo: string) {
  fireEvent.change(screen.getByLabelText('Código de invitación'), { target: { value: codigo } })
  fireEvent.click(screen.getByRole('button', { name: 'Inscribirme' }))
}

describe('Inscribirse', () => {
  it('envía el código tal como se escribió y muestra el curso con el enlace a sus problemas', async () => {
    api.inscribirse.mockResolvedValue({ ok: true, cuerpo: INSCRITA })
    render(<Inscribirse />)

    escribirYEnviar('abcd2345')

    expect(await screen.findByText(/Te inscribiste en/)).toBeTruthy()
    expect(api.inscribirse).toHaveBeenCalledWith('abcd2345')
    const enlace = screen.getByRole('link', { name: 'Ver los problemas del curso' })
    expect(enlace.getAttribute('href')).toBe('/cursos/7/problemas')
    expect(screen.queryByRole('button', { name: 'Inscribirme' })).toBeNull()
  })

  it('muestra el motivo del servidor cuando el código es incorrecto o venció', async () => {
    api.inscribirse.mockResolvedValue({
      ok: false,
      cuerpo: { detail: 'El código de invitación no es correcto. Revísalo con tu docente.' },
    })
    render(<Inscribirse />)

    escribirYEnviar('ZZZZ9999')

    expect(await screen.findByText('El código de invitación no es correcto. Revísalo con tu docente.')).toBeTruthy()
    // El formulario sigue ahí para corregir el código.
    expect(screen.getByRole('button', { name: 'Inscribirme' })).toBeTruthy()
  })

  it('avisa cuando el estudiante ya estaba inscrito', async () => {
    api.inscribirse.mockResolvedValue({ ok: false, cuerpo: { detail: 'Ya estás inscrito en este curso.' } })
    render(<Inscribirse />)

    escribirYEnviar('ABCD2345')

    expect(await screen.findByText('Ya estás inscrito en este curso.')).toBeTruthy()
  })

  it('usa un mensaje general cuando el servidor no explica el error', async () => {
    api.inscribirse.mockResolvedValue({ ok: false, cuerpo: {} })
    render(<Inscribirse />)

    escribirYEnviar('ABCD2345')

    expect(await screen.findByText('No se pudo completar la inscripción. Inténtalo de nuevo.')).toBeTruthy()
  })

  it('avisa cuando no hay conexión con el servidor', async () => {
    api.inscribirse.mockRejectedValue(new TypeError('Failed to fetch'))
    render(<Inscribirse />)

    escribirYEnviar('ABCD2345')

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeTruthy()
  })

  it('deshabilita el botón mientras se inscribe, para que un doble clic no envíe dos veces', async () => {
    let terminar: (valor: unknown) => void = () => {}
    api.inscribirse.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    render(<Inscribirse />)

    escribirYEnviar('ABCD2345')
    const boton = screen.getByRole('button', { name: 'Inscribirme' }) as HTMLButtonElement
    expect(boton.disabled).toBe(true)
    fireEvent.click(boton)
    expect(api.inscribirse).toHaveBeenCalledTimes(1)

    terminar({ ok: true, cuerpo: INSCRITA })
    expect(await screen.findByText(/Te inscribiste en/)).toBeTruthy()
  })
})
