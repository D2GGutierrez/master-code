import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import IndicadorGuardado from './IndicadorGuardado.tsx'

afterEach(cleanup)

describe('IndicadorGuardado', () => {
  it('es una región role="status" que se anuncia con cortesía', () => {
    render(<IndicadorGuardado estado="guardando" ultimoGuardado={null} />)
    const indicador = screen.getByRole('status')
    expect(indicador.getAttribute('aria-live')).toBe('polite')
  })

  it('muestra «Guardando…» y, al cambiar las props, la hora del guardado', () => {
    const { rerender } = render(<IndicadorGuardado estado="guardando" ultimoGuardado={null} />)
    expect(screen.getByRole('status').textContent).toBe('Guardando…')
    const hoy = new Date()
    hoy.setHours(9, 5, 0, 0)
    rerender(<IndicadorGuardado estado="guardado" ultimoGuardado={hoy} />)
    expect(screen.getByRole('status').textContent).toBe('Guardado a las 09:05')
  })

  it('sin nada que decir, la región queda vacía', () => {
    render(<IndicadorGuardado estado="inactivo" ultimoGuardado={null} />)
    expect(screen.getByRole('status').textContent).toBe('')
  })

  it('el error se dice con texto, no solo con color, y no es una alerta', () => {
    render(<IndicadorGuardado estado="error" ultimoGuardado={null} />)
    expect(screen.getByRole('status').textContent).toBe('No se pudo guardar el borrador')
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
