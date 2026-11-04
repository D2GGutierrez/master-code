import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProblemaDeLista } from '../api/problemas.ts'
import Inicio from './Inicio.tsx'

// La pantalla no habla con user-api en estas pruebas: se simula lo que responden los tres módulos de la API.
const api = vi.hoisted(() => ({
  misCursos: vi.fn(),
  listarProblemas: vi.fn(),
  obtenerAvance: vi.fn(),
  obtenerAvanceCurso: vi.fn(),
}))
vi.mock('../api/cursos.ts', () => ({ misCursos: api.misCursos }))
vi.mock('../api/problemas.ts', () => ({ listarProblemas: api.listarProblemas }))
vi.mock('../api/avance.ts', () => ({ obtenerAvance: api.obtenerAvance, obtenerAvanceCurso: api.obtenerAvanceCurso }))

// Avance de cada curso según user-api (HU43-T1): es lo que dibujan las tarjetas.
const AVANCE = new Map([
  [10, { curso_id: 10, total: 4, resueltos: 1, intentados: 1, pendientes: 2 }],
  [11, { curso_id: 11, total: 2, resueltos: 2, intentados: 0, pendientes: 0 }],
])

const PYTHON = { curso_id: 10, nombre: 'Algoritmos I', lenguaje: 'python', institucion: 'tecsup.edu.pe', fecha_inscripcion: '2026-10-02T15:00:00Z' }
const JAVA = { curso_id: 11, nombre: 'Estructuras en Java', lenguaje: 'java', institucion: 'tecsup.edu.pe', fecha_inscripcion: '2026-10-09T15:00:00Z' }

function problema(id: number, estado: ProblemaDeLista['estado'], titulo = `Problema ${id}`): ProblemaDeLista {
  return { id, titulo, lenguaje: 'python', dificultad: 'facil', plataforma: 'web', tipo_ejercicio: 'escritura_codigo', fecha_publicacion: '2026-10-28T15:00:00Z', estado }
}

// Una sesión con un access de mentira que lleva el correo, como el de admin-api.
function iniciarSesion(correo: string) {
  const cuerpo = btoa(JSON.stringify({ email: correo, exp: Math.floor(Date.now() / 1000) + 600 }))
  localStorage.setItem('access', `cabecera.${cuerpo}.firma`)
}

beforeEach(() => {
  iniciarSesion('ana.torres@tecsup.edu.pe')
  api.listarProblemas.mockImplementation(async (cursoId: number) => ({
    ok: true,
    problemas:
      cursoId === 10
        ? [problema(1, 'resuelto'), problema(2, 'intentado', 'Eco'), problema(3, 'sin_abrir'), problema(4, 'sin_abrir')]
        : [problema(5, 'resuelto'), problema(6, 'resuelto')],
  }))
  api.obtenerAvance.mockResolvedValue({
    problemas_intentados: 2,
    total_envios: 2,
    desde_web: 2,
    desde_movil: 0,
    problemas: [{ problema_id: 2, envios: 1, desde_web: 1, desde_movil: 0, ultimo_envio: '2026-10-29T10:00:00Z' }],
  })
  api.obtenerAvanceCurso.mockImplementation(async (cursoId: number) => AVANCE.get(cursoId))
})

afterEach(() => {
  cleanup()
  localStorage.clear()
  for (const funcion of Object.values(api)) {
    funcion.mockReset()
  }
})

describe('Inicio', () => {
  it('saluda por el nombre del correo y muestra cada curso con su progreso real', async () => {
    api.misCursos.mockResolvedValue([PYTHON, JAVA])
    render(<Inicio />)

    expect((await screen.findByRole('heading', { level: 1 })).textContent).toBe('¡Hola, Ana!')
    const enlace = await screen.findByRole('link', { name: /Algoritmos I/ })
    expect(enlace.getAttribute('href')).toBe('/cursos/10/problemas')
    // Python: 1 de 4 resueltos; Java: 2 de 2. Cada curso sale en su tarjeta y el de Python también en el panel.
    expect((await screen.findAllByRole('progressbar', { name: /Algoritmos I: 1 de 4 problemas resueltos/ })).length).toBeGreaterThan(0)
    expect(await screen.findByRole('progressbar', { name: /Estructuras en Java: 2 de 2 problemas resueltos/ })).toBeTruthy()
    expect(screen.getByText('Completado')).toBeTruthy()
  })

  it('«Continuar» lleva al siguiente problema del curso donde se quedó el estudiante', async () => {
    api.misCursos.mockResolvedValue([JAVA, PYTHON])
    render(<Inicio />)

    // El último envío fue del problema 2, que es de Python (aunque Java sea el primer curso de la lista).
    const continuar = await screen.findByRole('link', { name: 'Continuar' })
    expect(continuar.getAttribute('href')).toBe('/problemas/2')
    expect(screen.getByText('Eco')).toBeTruthy()
    expect(screen.getByText('Continúa donde lo dejaste')).toBeTruthy()
  })

  it('el filtro por lenguaje deja solo los cursos de ese lenguaje', async () => {
    api.misCursos.mockResolvedValue([PYTHON, JAVA])
    render(<Inicio />)
    await screen.findByRole('link', { name: /Estructuras en Java/ })

    fireEvent.click(screen.getByRole('button', { name: /^Java/ }))

    expect(screen.queryByRole('link', { name: /Algoritmos I/ })).toBeNull()
    expect(screen.getByRole('link', { name: /Estructuras en Java/ })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Java/ }).getAttribute('aria-pressed')).toBe('true')

    fireEvent.click(screen.getByRole('button', { name: /^Todos/ }))
    expect(screen.getByRole('link', { name: /Algoritmos I/ })).toBeTruthy()
  })

  it('con un solo lenguaje no ofrece filtro', async () => {
    api.misCursos.mockResolvedValue([PYTHON])
    render(<Inicio />)
    await screen.findByRole('link', { name: /Algoritmos I/ })
    expect(screen.queryByRole('group', { name: 'Filtrar por lenguaje' })).toBeNull()
  })

  it('(a) cada tarjeta muestra el avance del servidor: su texto y su barra', async () => {
    api.misCursos.mockResolvedValue([PYTHON, JAVA])
    render(<Inicio />)

    const python = await screen.findByRole('link', { name: /Algoritmos I/ })
    expect(await within(python).findByText('1 resuelto · 1 intentado · 2 pendientes')).toBeTruthy()
    expect(within(python).getByRole('progressbar').getAttribute('aria-valuenow')).toBe('25')
    const java = screen.getByRole('link', { name: /Estructuras en Java/ })
    expect(within(java).getByText('2 resueltos · 0 intentados · 0 pendientes')).toBeTruthy()
    expect(within(java).getByRole('progressbar').getAttribute('aria-valuenow')).toBe('100')
    expect(within(java).getByText('Repasar')).toBeTruthy()
  })

  it('(b) mientras llega el avance, la tarjeta muestra el esqueleto y ningún número', async () => {
    api.misCursos.mockResolvedValue([PYTHON])
    api.obtenerAvanceCurso.mockReturnValue(new Promise(() => {}))
    render(<Inicio />)

    const enlace = await screen.findByRole('link', { name: /Algoritmos I/ })
    expect(enlace.querySelector('.esqueleto')).not.toBeNull()
    expect(within(enlace).queryByRole('progressbar')).toBeNull()
    expect(within(enlace).queryByText(/resuelto/)).toBeNull()
  })

  it('(c) si falla el avance, las tarjetas van sin progreso, con un aviso y «Reintentar»', async () => {
    api.misCursos.mockResolvedValue([PYTHON])
    api.obtenerAvanceCurso.mockRejectedValueOnce(new Error('sin conexión'))
    render(<Inicio />)

    expect((await screen.findByRole('alert')).textContent).toContain('No se pudo cargar tu progreso. Inténtalo de nuevo.')
    const enlace = screen.getByRole('link', { name: /Algoritmos I/ })
    expect(within(enlace).queryByRole('progressbar')).toBeNull()
    expect(enlace.querySelector('.esqueleto')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await within(enlace).findByText('1 resuelto · 1 intentado · 2 pendientes')).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('(d) un curso sin problemas dice «Sin problemas aún» y ofrece «Abrir curso»', async () => {
    api.misCursos.mockResolvedValue([JAVA])
    api.obtenerAvanceCurso.mockResolvedValue({ curso_id: 11, total: 0, resueltos: 0, intentados: 0, pendientes: 0 })
    render(<Inicio />)

    const enlace = await screen.findByRole('link', { name: /Estructuras en Java/ })
    expect(await within(enlace).findByText('Sin problemas aún')).toBeTruthy()
    expect(within(enlace).getByText('Abrir curso')).toBeTruthy()
    expect(within(enlace).queryByRole('progressbar')).toBeNull()
  })

  it('(e) las tarjetas no dependen de las listas de problemas: piden el avance de cada curso', async () => {
    api.misCursos.mockResolvedValue([PYTHON, JAVA])
    // Aunque las listas fallen (solo las usa el panel «Continúa»), las tarjetas muestran el avance del servidor.
    api.listarProblemas.mockRejectedValue(new Error('sin conexión'))
    render(<Inicio />)

    const python = await screen.findByRole('link', { name: /Algoritmos I/ })
    expect(await within(python).findByText('1 resuelto · 1 intentado · 2 pendientes')).toBeTruthy()
    expect(api.obtenerAvanceCurso).toHaveBeenCalledTimes(2)
    expect(api.obtenerAvanceCurso.mock.calls.map(([cursoId]) => cursoId).sort()).toEqual([10, 11])
  })

  it('avisa cuando no se pueden cargar los cursos y deja reintentar', async () => {
    api.misCursos.mockRejectedValueOnce(new Error('caído')).mockResolvedValue([PYTHON])
    render(<Inicio />)

    expect(await screen.findByRole('alert')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('link', { name: /Algoritmos I/ })).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('sin cursos muestra el mensaje y no el panel «Continuar»', async () => {
    api.misCursos.mockResolvedValue([])
    render(<Inicio />)

    expect(await screen.findByText('Todavía no estás inscrito en ningún curso.')).toBeTruthy()
    expect(screen.queryByText('Continúa donde lo dejaste')).toBeNull()
    expect(screen.getByRole('link', { name: 'Inscribirme con un código' }).getAttribute('href')).toBe('/cursos/inscribirse')
  })

  it('saluda sin nombre cuando el correo no parece uno', async () => {
    iniciarSesion('u202112345@tecsup.edu.pe')
    api.misCursos.mockResolvedValue([PYTHON])
    render(<Inicio />)

    expect((await screen.findByRole('heading', { level: 1 })).textContent).toBe('¡Hola de nuevo!')
  })
})
