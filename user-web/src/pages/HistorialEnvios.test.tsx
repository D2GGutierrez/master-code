import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { EnvioResumen } from '../api/envios.ts'
import HistorialEnvios from './HistorialEnvios.tsx'

// La pantalla no habla con user-api en estas pruebas: se simula lo que responden las dos funciones que usa.
const api = vi.hoisted(() => ({ listarEnvios: vi.fn(), obtenerProblema: vi.fn() }))
vi.mock('../api/envios.ts', () => ({ listarEnvios: api.listarEnvios }))
vi.mock('../api/problemas.ts', () => ({ obtenerProblema: api.obtenerProblema }))

const PROBLEMA = {
  id: 7,
  curso_id: 1,
  titulo: 'Suma',
  enunciado: 'Suma dos números.',
  lenguaje: 'python',
  dificultad: 'facil',
  plataforma: 'web',
  tipo_ejercicio: 'escritura_codigo',
  codigo_base: '',
  casos_prueba: [],
}

// Del más reciente al más antiguo, como los entrega user-api; sin número de intento: lo calcula la pantalla.
function envios(cantidad: number): EnvioResumen[] {
  return Array.from({ length: cantidad }, (_, indice) => ({
    id: cantidad - indice,
    numero_intento: null,
    fecha_envio: '2026-11-10T10:00:00Z',
    resultado: 'correcto',
    puntaje: 100,
  }))
}

// El texto de la primera celda de cada fila: «N.º 25», «N.º 24»…
function intentos() {
  return within(screen.getByRole('region', { name: 'Historial de envíos' }))
    .getAllByRole('row')
    .slice(1)
    .map((fila) => fila.querySelector('td')?.textContent)
}

async function abrir(ruta = '/problemas/7/envios') {
  window.history.replaceState(null, '', ruta)
  render(<HistorialEnvios id={7} />)
  await screen.findByRole('heading', { name: 'Suma' })
}

const navegacion = () => screen.getByRole('navigation', { name: 'Paginación' })

beforeEach(() => {
  localStorage.setItem('access', 'token-de-prueba')
  // jsdom no desplaza la página: basta con que la llamada exista.
  Element.prototype.scrollIntoView = vi.fn()
  api.obtenerProblema.mockResolvedValue({ ok: true, problema: PROBLEMA })
  api.listarEnvios.mockResolvedValue({ ok: true, envios: envios(25) })
})

afterEach(() => {
  cleanup()
  localStorage.clear()
  window.history.replaceState(null, '', '/')
  api.listarEnvios.mockReset()
  api.obtenerProblema.mockReset()
})

describe('HistorialEnvios', () => {
  it('(a) muestra 10 por página y la numeración sigue el orden de la lista completa', async () => {
    await abrir()
    expect(intentos()).toEqual(Array.from({ length: 10 }, (_, i) => `N.º ${25 - i}`))
    expect(within(navegacion()).getByText('Página 1 de 3')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(intentos()).toEqual(Array.from({ length: 10 }, (_, i) => `N.º ${15 - i}`))
    expect(within(navegacion()).getByText('Página 2 de 3')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(intentos()[0]).toBe('N.º 25')
  })

  it('(b) en la última página «Siguiente» queda con aria-disabled y no pierde el foco', async () => {
    await abrir()
    const siguiente = screen.getByRole('button', { name: 'Siguiente' })
    siguiente.focus()
    fireEvent.click(siguiente)
    fireEvent.click(siguiente)
    expect(intentos()).toEqual(['N.º 5', 'N.º 4', 'N.º 3', 'N.º 2', 'N.º 1'])
    expect(siguiente.getAttribute('aria-disabled')).toBe('true')
    expect(siguiente.hasAttribute('disabled')).toBe(false)
    fireEvent.click(siguiente)
    expect(within(navegacion()).getByText('Página 3 de 3')).toBeTruthy()
    expect(document.activeElement).toBe(siguiente)
    expect(screen.getByRole('button', { name: 'Anterior' }).getAttribute('aria-disabled')).toBeNull()
  })

  it('(c) con 10 envíos o menos no hay controles', async () => {
    api.listarEnvios.mockResolvedValue({ ok: true, envios: envios(10) })
    await abrir()
    expect(intentos()).toHaveLength(10)
    expect(screen.queryByRole('navigation', { name: 'Paginación' })).toBeNull()
    expect(screen.queryByText(/Mostrando/)).toBeNull()
  })

  it('(d) anuncia el rango que se muestra', async () => {
    await abrir()
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    const anuncio = screen.getByText('Mostrando 11–20 de 25 envíos')
    expect(anuncio.getAttribute('aria-live')).toBe('polite')
  })

  it('(e) arranca en la página de la URL y limita una página fuera de rango', async () => {
    await abrir('/problemas/7/envios?pagina=2')
    expect(intentos()[0]).toBe('N.º 15')
    cleanup()

    await abrir('/problemas/7/envios?pagina=99')
    expect(within(navegacion()).getByText('Página 3 de 3')).toBeTruthy()
    expect(intentos()[0]).toBe('N.º 5')
  })

  it('(f) cambiar de página actualiza la URL sin recargar; la página 1 va sin parámetro', async () => {
    await abrir('/problemas/7/envios')
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(window.location.pathname).toBe('/problemas/7/envios')
    expect(window.location.search).toBe('?pagina=2')
    fireEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(window.location.search).toBe('')
    // Sin recarga: la pantalla no volvió a pedir nada.
    expect(api.listarEnvios).toHaveBeenCalledTimes(1)
  })

  it('(g) la lista vacía, el error y el 404 se ven como antes', async () => {
    api.listarEnvios.mockResolvedValue({ ok: true, envios: [] })
    await abrir()
    expect(screen.getByText('Aún no has enviado ninguna solución a este problema.')).toBeTruthy()
    expect(screen.queryByRole('navigation', { name: 'Paginación' })).toBeNull()
    cleanup()

    api.listarEnvios.mockRejectedValue(new Error('sin conexión'))
    window.history.replaceState(null, '', '/problemas/7/envios')
    render(<HistorialEnvios id={7} />)
    expect((await screen.findByRole('alert')).textContent).toBe('No se pudo cargar el historial. Inténtalo de nuevo.')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeTruthy()
    cleanup()

    api.obtenerProblema.mockResolvedValue({ ok: false, status: 404 })
    render(<HistorialEnvios id={7} />)
    expect((await screen.findByRole('alert')).textContent).toBe('El problema no existe o no está disponible.')
  })
})
