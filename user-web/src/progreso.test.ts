import { describe, expect, it } from 'vitest'
import type { Avance } from './api/avance.ts'
import type { MiCurso } from './api/cursos.ts'
import type { ProblemaDeLista } from './api/problemas.ts'
import { elegirContinuar, estadoDelCurso, resumir, resumirTodos, siguienteProblema, type ProblemasPorCurso } from './progreso.ts'

function problema(id: number, estado: ProblemaDeLista['estado'], extra: Partial<ProblemaDeLista> = {}): ProblemaDeLista {
  return {
    id,
    titulo: `Problema ${id}`,
    lenguaje: 'python',
    dificultad: 'facil',
    plataforma: 'web',
    tipo_ejercicio: 'escritura_codigo',
    fecha_publicacion: '2026-10-28T15:00:00Z',
    estado,
    ...extra,
  }
}

function curso(id: number): MiCurso {
  return { curso_id: id, nombre: `Curso ${id}`, lenguaje: 'python', institucion: 'tecsup.edu.pe', fecha_inscripcion: '2026-10-01T10:00:00Z' }
}

function avanceCon(...envios: [number, string][]): Avance {
  return {
    problemas_intentados: envios.length,
    total_envios: envios.length,
    desde_web: envios.length,
    desde_movil: 0,
    problemas: envios.map(([problema_id, ultimo_envio]) => ({ problema_id, envios: 1, desde_web: 1, desde_movil: 0, ultimo_envio })),
  }
}

describe('resumir', () => {
  it('cuenta resueltos, intentados y sin abrir, y da la fracción resuelta', () => {
    const resumen = resumir([problema(1, 'resuelto'), problema(2, 'intentado'), problema(3, 'sin_abrir'), problema(4, 'resuelto')])
    expect(resumen).toEqual({ total: 4, resueltos: 2, intentados: 1, sinAbrir: 1, fraccion: 0.5 })
  })

  it('un curso sin problemas tiene progreso 0 y no divide entre cero', () => {
    expect(resumir([])).toEqual({ total: 0, resueltos: 0, intentados: 0, sinAbrir: 0, fraccion: 0 })
  })
})

describe('estadoDelCurso', () => {
  it('distingue sin problemas, sin empezar, en curso y completo', () => {
    expect(estadoDelCurso(resumir([]))).toBe('sin-problemas')
    expect(estadoDelCurso(resumir([problema(1, 'sin_abrir')]))).toBe('nuevo')
    expect(estadoDelCurso(resumir([problema(1, 'intentado'), problema(2, 'sin_abrir')]))).toBe('en-curso')
    expect(estadoDelCurso(resumir([problema(1, 'resuelto'), problema(2, 'sin_abrir')]))).toBe('en-curso')
    expect(estadoDelCurso(resumir([problema(1, 'resuelto'), problema(2, 'resuelto')]))).toBe('completo')
  })
})

describe('siguienteProblema', () => {
  it('es null cuando todo está resuelto o no hay problemas', () => {
    expect(siguienteProblema([])).toBeNull()
    expect(siguienteProblema([problema(1, 'resuelto')])).toBeNull()
  })

  it('retoma lo ya intentado antes que lo sin abrir, y lo más fácil antes que lo difícil', () => {
    const lista = [
      problema(1, 'sin_abrir', { dificultad: 'facil' }),
      problema(2, 'intentado', { dificultad: 'dificil' }),
      problema(3, 'intentado', { dificultad: 'medio' }),
    ]
    expect(siguienteProblema(lista)?.id).toBe(3)
  })

  it('prefiere un problema que se resuelve con el editor a uno que no', () => {
    const lista = [
      problema(1, 'intentado', { tipo_ejercicio: 'opcion_multiple' }),
      problema(2, 'sin_abrir', { dificultad: 'dificil' }),
    ]
    expect(siguienteProblema(lista)?.id).toBe(2)
  })

  it('no modifica la lista que recibe', () => {
    const lista = [problema(2, 'sin_abrir', { dificultad: 'dificil' }), problema(1, 'sin_abrir')]
    siguienteProblema(lista)
    expect(lista.map((p) => p.id)).toEqual([2, 1])
  })
})

describe('resumirTodos', () => {
  it('suma los cursos que cargaron e ignora los que no', () => {
    const problemas: ProblemasPorCurso = new Map([
      [1, [problema(1, 'resuelto'), problema(2, 'sin_abrir')]],
      [2, null],
      [3, [problema(3, 'intentado')]],
    ])
    expect(resumirTodos(problemas)).toMatchObject({ total: 3, resueltos: 1, intentados: 1 })
  })
})

describe('elegirContinuar', () => {
  const cursos = [curso(10), curso(11)]
  const problemas: ProblemasPorCurso = new Map([
    [10, [problema(1, 'resuelto'), problema(2, 'sin_abrir')]],
    [11, [problema(3, 'intentado'), problema(4, 'sin_abrir')]],
  ])

  it('es null si no hay cursos', () => {
    expect(elegirContinuar([], new Map(), null)).toBeNull()
  })

  it('elige el curso del envío más reciente y su siguiente problema', () => {
    const avance = avanceCon([1, '2026-10-25T10:00:00Z'], [3, '2026-10-29T10:00:00Z'])
    const resultado = elegirContinuar(cursos, problemas, avance)
    expect(resultado?.curso.curso_id).toBe(11)
    expect(resultado?.problema?.id).toBe(3)
    expect(resultado?.conActividad).toBe(true)
  })

  it('sin envíos, empieza por el primer curso que aún tenga algo por resolver', () => {
    const casi = new Map([
      [10, [problema(1, 'resuelto')]],
      [11, [problema(3, 'sin_abrir')]],
    ]) as ProblemasPorCurso
    const resultado = elegirContinuar(cursos, casi, null)
    expect(resultado?.curso.curso_id).toBe(11)
    expect(resultado?.conActividad).toBe(false)
  })

  it('ignora envíos de problemas que no están en ningún curso conocido', () => {
    const resultado = elegirContinuar(cursos, problemas, avanceCon([999, '2026-10-29T10:00:00Z']))
    expect(resultado?.conActividad).toBe(false)
    expect(resultado?.curso.curso_id).toBe(10)
  })

  it('si el último curso está completo, no hay siguiente problema', () => {
    const completo = new Map([[10, [problema(1, 'resuelto')]]]) as ProblemasPorCurso
    const resultado = elegirContinuar([curso(10)], completo, avanceCon([1, '2026-10-29T10:00:00Z']))
    expect(resultado?.problema).toBeNull()
  })

  it('aguanta listas que no cargaron', () => {
    const sinListas: ProblemasPorCurso = new Map([[10, null]])
    const resultado = elegirContinuar(cursos, sinListas, null)
    expect(resultado?.curso.curso_id).toBe(10)
    expect(resultado?.problema).toBeNull()
  })
})
