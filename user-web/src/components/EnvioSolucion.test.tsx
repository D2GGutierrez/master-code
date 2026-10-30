import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { EnvioCalificado } from '../api/envios.ts'
import EnvioSolucion from './EnvioSolucion.tsx'

// El componente no habla con user-api en estas pruebas: se simula lo que responde enviarSolucion().
const api = vi.hoisted(() => ({ enviarSolucion: vi.fn() }))
vi.mock('../api/envios.ts', () => api)

const CORRECTO: EnvioCalificado = {
  id: 1,
  numero_intento: 1,
  resultado: 'correcto',
  puntaje: 100,
  casos_superados: 2,
  casos_total: 2,
  aviso: null,
  casos: [
    { publico: true, paso: true, entrada: '2 3', salida_esperada: '5', salida_obtenida: '5', estado: 'ok' },
    { publico: false, paso: true },
  ],
}

// Un caso público que falla en la segunda línea y uno oculto que también falla.
const PARCIAL: EnvioCalificado = {
  id: 2,
  numero_intento: 3,
  resultado: 'parcial',
  puntaje: 66.67,
  casos_superados: 2,
  casos_total: 3,
  aviso: null,
  casos: [
    { publico: true, paso: false, entrada: '2 3', salida_esperada: '5\n7', salida_obtenida: '5\n8', estado: 'ok' },
    { publico: false, paso: false },
    { publico: false, paso: true },
  ],
}

afterEach(() => {
  cleanup()
  api.enviarSolucion.mockReset()
})

function renderizar(codigo = 'print(5)') {
  return render(<EnvioSolucion problemaId={7} codigo={codigo} />)
}

function pulsarEnviar() {
  fireEvent.click(screen.getByRole('button', { name: 'Enviar solución' }))
}

function enviarYConfirmar() {
  pulsarEnviar()
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar envío' }))
}

describe('EnvioSolucion', () => {
  it('sin código no pide confirmación ni llama al servidor', () => {
    renderizar('   ')

    pulsarEnviar()

    expect(screen.getByText('Escribe tu código antes de enviarlo.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Confirmar envío' })).toBeNull()
    expect(api.enviarSolucion).not.toHaveBeenCalled()
  })

  it('pide confirmación antes de enviar y Cancelar no envía nada', () => {
    renderizar()

    pulsarEnviar()

    expect(screen.getByText(/contará como un intento oficial/)).toBeTruthy()
    expect(api.enviarSolucion).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(screen.getByRole('button', { name: 'Enviar solución' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Confirmar envío' })).toBeNull()
    expect(api.enviarSolucion).not.toHaveBeenCalled()
  })

  it('al confirmar envía el código al problema y muestra resultado, intento, puntaje y casos', async () => {
    api.enviarSolucion.mockResolvedValue({ ok: true, envio: CORRECTO })
    renderizar('print(5)')

    enviarYConfirmar()

    expect(await screen.findByText('Resultado: Correcto')).toBeTruthy()
    expect(api.enviarSolucion).toHaveBeenCalledTimes(1)
    expect(api.enviarSolucion).toHaveBeenCalledWith(7, 'print(5)')
    expect(screen.getByText('Intento n.º 1 · Puntaje: 100 % · Casos superados: 2 de 2')).toBeTruthy()
    expect(screen.getByText(/Caso 1: pasó/)).toBeTruthy()
    expect(screen.getByText(/Caso 2 \(oculto\): pasó/)).toBeTruthy()
  })

  it('mientras evalúa bloquea el botón: un clic más no envía otra vez', async () => {
    let terminar: (valor: unknown) => void = () => {}
    api.enviarSolucion.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    renderizar()

    enviarYConfirmar()

    expect(screen.getByRole('status').textContent).toBe('Evaluando tu solución…')
    const boton = screen.getByRole('button', { name: 'Enviar solución' }) as HTMLButtonElement
    expect(boton.disabled).toBe(true)
    fireEvent.click(boton)
    expect(screen.queryByRole('button', { name: 'Confirmar envío' })).toBeNull()
    expect(api.enviarSolucion).toHaveBeenCalledTimes(1)

    terminar({ ok: true, envio: CORRECTO })

    expect(await screen.findByText('Resultado: Correcto')).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Enviar solución' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('de un caso oculto muestra solo pasó o falló; del público, entrada, salidas y la línea distinta', async () => {
    api.enviarSolucion.mockResolvedValue({ ok: true, envio: PARCIAL })
    const { container } = renderizar()

    enviarYConfirmar()

    expect(await screen.findByText('Resultado: Parcial')).toBeTruthy()
    expect(screen.getByText('Intento n.º 3 · Puntaje: 66.67 % · Casos superados: 2 de 3')).toBeTruthy()
    const [publico, oculto] = screen.getAllByRole('listitem')
    expect(within(publico).getByText('2 3')).toBeTruthy()
    expect(within(publico).getByText('Salida esperada')).toBeTruthy()
    expect(within(publico).getByText('Tu salida')).toBeTruthy()
    // Solo las líneas que no coinciden quedan marcadas: el 7 esperado y el 8 obtenido.
    expect(Array.from(container.querySelectorAll('mark')).map((marca) => marca.textContent)).toEqual(['7', '8'])
    expect(oculto.textContent).toBe('Caso 2 (oculto): falló')
    expect(within(oculto).queryByText('Ver detalle')).toBeNull()
  })

  it('muestra el aviso del código y el error del caso público', async () => {
    api.enviarSolucion.mockResolvedValue({
      ok: true,
      envio: {
        ...PARCIAL,
        resultado: 'incorrecto',
        puntaje: 0,
        casos_superados: 0,
        aviso: 'Tu código no compiló. SyntaxError en la línea 2',
        casos: [
          { publico: true, paso: false, entrada: '', salida_esperada: '5', salida_obtenida: '', error: 'SyntaxError en la línea 2', estado: 'error_compilacion' },
          { publico: false, paso: false },
        ],
      },
    })
    renderizar()

    enviarYConfirmar()

    expect(await screen.findByText('Tu código no compiló. SyntaxError en la línea 2')).toBeTruthy()
    expect(screen.getByText('SyntaxError en la línea 2')).toBeTruthy()
    expect(screen.getByText('(sin entrada)')).toBeTruthy()
  })

  it('un envío nuevo reemplaza el resultado anterior', async () => {
    api.enviarSolucion.mockResolvedValueOnce({ ok: true, envio: PARCIAL }).mockResolvedValueOnce({ ok: true, envio: CORRECTO })
    renderizar()

    enviarYConfirmar()
    expect(await screen.findByText('Resultado: Parcial')).toBeTruthy()
    enviarYConfirmar()

    expect(await screen.findByText('Resultado: Correcto')).toBeTruthy()
    expect(screen.queryByText('Resultado: Parcial')).toBeNull()
    expect(api.enviarSolucion).toHaveBeenCalledTimes(2)
  })

  it('un 503 muestra el texto del servidor y deja volver a enviar', async () => {
    api.enviarSolucion.mockResolvedValue({
      ok: false,
      status: 503,
      detail: 'El servicio de ejecución no está disponible en este momento. Tu envío no se contó: intenta de nuevo en un momento.',
    })
    renderizar()

    enviarYConfirmar()

    expect(await screen.findByText(/Tu envío no se contó/)).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Enviar solución' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('usa un mensaje propio para un 404 y uno general para un error inesperado', async () => {
    api.enviarSolucion.mockResolvedValueOnce({ ok: false, status: 404, detail: 'Problema no encontrado.' })
    renderizar()

    enviarYConfirmar()
    expect(await screen.findByText('El problema no está disponible o no estás inscrito en su curso.')).toBeTruthy()

    api.enviarSolucion.mockResolvedValueOnce({ ok: false, status: 500 })
    enviarYConfirmar()
    expect(await screen.findByText('No se pudo enviar tu solución. Inténtalo de nuevo.')).toBeTruthy()
  })

  it('sin conexión con el servidor lo dice y deja volver a enviar', async () => {
    api.enviarSolucion.mockRejectedValue(new TypeError('Failed to fetch'))
    renderizar()

    enviarYConfirmar()

    expect(await screen.findByText('No se pudo contactar al servidor. Revisa tu conexión e inténtalo de nuevo.')).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Enviar solución' }) as HTMLButtonElement).disabled).toBe(false)
  })
})
