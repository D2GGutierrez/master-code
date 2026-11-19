import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as cuentas from '../api/cuentas.ts'
import VerificarCorreo from './VerificarCorreo.tsx'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function abrirCon(busqueda: string) {
  window.history.pushState({}, '', `/verificar-correo${busqueda}`)
}

describe('VerificarCorreo', () => {
  beforeEach(() => {
    vi.spyOn(cuentas, 'verificarCorreo')
    vi.spyOn(cuentas, 'reenviarCodigo')
  })

  it('con el correo en la URL solo pide el código y dice a dónde se envió', () => {
    abrirCon('?email=Ana@Tecsup.edu.pe')
    render(<VerificarCorreo />)

    expect(screen.getByText('ana@tecsup.edu.pe')).toBeTruthy()
    expect(screen.getByLabelText('Código de 6 dígitos')).toBeTruthy()
    expect(screen.queryByLabelText('Correo institucional')).toBeNull()
  })

  it('sin correo en la URL también lo pide', () => {
    abrirCon('')
    render(<VerificarCorreo />)

    expect(screen.getByLabelText('Correo institucional')).toBeTruthy()
  })

  it('envía el correo y el código; si sale bien muestra el mensaje y el enlace al login', async () => {
    abrirCon('?email=ana@tecsup.edu.pe')
    vi.mocked(cuentas.verificarCorreo).mockResolvedValue({ ok: true, cuerpo: { detail: 'Correo verificado. Ya puedes iniciar sesión.' } })
    render(<VerificarCorreo />)

    fireEvent.change(screen.getByLabelText('Código de 6 dígitos'), { target: { value: ' 123456 ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar correo' }))

    await waitFor(() => expect(screen.getByText('Correo verificado. Ya puedes iniciar sesión.')).toBeTruthy())
    expect(cuentas.verificarCorreo).toHaveBeenCalledWith('ana@tecsup.edu.pe', '123456')
    expect(screen.getByRole('link', { name: 'Ir a iniciar sesión' }).getAttribute('href')).toBe('/login')
  })

  it('si el código es incorrecto muestra el motivo del servidor', async () => {
    abrirCon('?email=ana@tecsup.edu.pe')
    vi.mocked(cuentas.verificarCorreo).mockResolvedValue({ ok: false, cuerpo: { detail: 'El código no es correcto.' } })
    render(<VerificarCorreo />)

    fireEvent.change(screen.getByLabelText('Código de 6 dígitos'), { target: { value: '000000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar correo' }))

    await waitFor(() => expect(screen.getByText('El código no es correcto.')).toBeTruthy())
  })

  it('«Pedir otro código» espera un minuto tras el registro', () => {
    abrirCon('?email=ana@tecsup.edu.pe')
    render(<VerificarCorreo />)

    const boton = screen.getByRole('button', { name: /Pedir otro código/ }) as HTMLButtonElement
    expect(boton.disabled).toBe(true)
    expect(boton.textContent).toContain('60 s')
  })

  it('sin conexión avisa en vez de quedarse cargando', async () => {
    abrirCon('?email=ana@tecsup.edu.pe')
    vi.mocked(cuentas.verificarCorreo).mockRejectedValue(new TypeError('Failed to fetch'))
    render(<VerificarCorreo />)

    fireEvent.change(screen.getByLabelText('Código de 6 dígitos'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar correo' }))

    await waitFor(() => expect(screen.getByText(/No se pudo conectar con el servidor/)).toBeTruthy())
  })
})
