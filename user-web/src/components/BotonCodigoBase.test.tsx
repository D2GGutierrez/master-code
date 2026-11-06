import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { codigoInicial } from '../codigoInicial.ts'
import BotonCodigoBase from './BotonCodigoBase.tsx'
import PanelEditor from './PanelEditor.tsx'

// Últimos props que llegaron a Monaco: así se ve el valor exacto que recibe el editor.
const monaco = vi.hoisted(() => ({ valor: '' }))

// Monaco necesita un navegador real: se sustituye por un textarea, como en EditorCodigo.test.tsx.
vi.mock('@monaco-editor/react', () => ({
  default: (props: { value?: string; onChange?: (texto: string | undefined) => void }) => {
    monaco.valor = props.value ?? ''
    return (
      <textarea aria-label="Monaco simulado" value={props.value} onChange={(e) => props.onChange?.(e.target.value)} />
    )
  },
}))
// El panel también monta el ejecutor y el envío: aquí no hablan con user-api.
vi.mock('../api/ejecucion.ts', () => ({ ejecutarCodigo: vi.fn() }))
vi.mock('../api/envios.ts', () => ({ enviarSolucion: vi.fn(), listarEnvios: vi.fn() }))

const BASE = 'def suma(a, b):\n    pass\n'

afterEach(cleanup)

function dibujar(codigoActual = 'print(1)', codigoBase: string | null | undefined = BASE) {
  const alRestaurar = vi.fn()
  render(<BotonCodigoBase codigoBase={codigoBase} codigoActual={codigoActual} alRestaurar={alRestaurar} />)
  return alRestaurar
}

const boton = () => screen.getByRole('button', { name: 'Volver al código base original' }) as HTMLButtonElement

describe('BotonCodigoBase', () => {
  it.each([null, undefined, '', '  \n '])('(a) sin código base (%j) no dibuja ningún botón', (codigoBase) => {
    // Sin dibujar(): con undefined, su valor por defecto pondría un código base.
    render(<BotonCodigoBase codigoBase={codigoBase} codigoActual="x" alRestaurar={vi.fn()} />)
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('(b) con un código distinto del código base, el botón está habilitado', () => {
    dibujar()
    expect(boton().disabled).toBe(false)
  })

  it('(c) con el código igual al código base, el botón está deshabilitado', () => {
    dibujar(BASE)
    expect(boton().disabled).toBe(true)
  })

  it('(d) al pulsarlo pide confirmación sin restaurar todavía', () => {
    const alRestaurar = dibujar()
    fireEvent.click(boton())
    const confirmacion = screen.getByRole('group', { name: '¿Volver al código base original?' })
    expect(confirmacion.textContent).toContain('Se reemplazará el código que hay ahora en el editor')
    expect(alRestaurar).not.toHaveBeenCalled()
  })

  it('(e) «Cancelar» cierra sin restaurar y devuelve el foco al botón', () => {
    const alRestaurar = dibujar()
    fireEvent.click(boton())
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(screen.queryByRole('group')).toBeNull()
    expect(alRestaurar).not.toHaveBeenCalled()
    expect(document.activeElement).toBe(boton())
  })

  it('(f) Escape cierra sin restaurar', () => {
    const alRestaurar = dibujar()
    fireEvent.click(boton())
    fireEvent.keyDown(screen.getByRole('button', { name: 'Cancelar' }), { key: 'Escape' })
    expect(screen.queryByRole('group')).toBeNull()
    expect(alRestaurar).not.toHaveBeenCalled()
  })

  it('(g) «Volver al código base» restaura una vez y cierra', () => {
    const alRestaurar = dibujar()
    fireEvent.click(boton())
    fireEvent.click(screen.getByRole('button', { name: 'Volver al código base' }))
    expect(alRestaurar).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('group')).toBeNull()
  })

  it('(h) un doble clic rápido restaura una sola vez', () => {
    const alRestaurar = dibujar()
    fireEvent.click(boton())
    const confirmar = screen.getByRole('button', { name: 'Volver al código base' })
    confirmar.click()
    confirmar.click()
    expect(alRestaurar).toHaveBeenCalledTimes(1)
  })

  it('(i) tras restaurar anuncia «Código base restaurado.»', () => {
    dibujar()
    fireEvent.click(boton())
    fireEvent.click(screen.getByRole('button', { name: 'Volver al código base' }))
    expect(screen.getByRole('status').textContent).toBe('Código base restaurado.')
  })

  it('(j) al abrir la confirmación el foco está en «Cancelar»', () => {
    dibujar()
    fireEvent.click(boton())
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Cancelar' }))
  })
})

// El estado del código vive en EditorProblema; aquí un contenedor mínimo con la misma función de restauración.
function PanelConEstado() {
  const [codigo, setCodigo] = useState('lo que escribió el estudiante')
  return (
    <PanelEditor
      problemaId={7}
      lenguaje="python"
      codigo={codigo}
      onCambioCodigo={setCodigo}
      codigoBase={BASE}
      alRestaurarCodigoBase={() => setCodigo(codigoInicial(BASE))}
    />
  )
}

describe('PanelEditor', () => {
  it('al confirmar, el editor recibe el código base exacto, con su sangría', () => {
    render(<PanelConEstado />)
    expect(monaco.valor).toBe('lo que escribió el estudiante')
    fireEvent.click(boton())
    fireEvent.click(screen.getByRole('button', { name: 'Volver al código base' }))
    expect(monaco.valor).toBe(BASE)
    // Ya no hay nada que restaurar.
    expect(boton().disabled).toBe(true)
  })
})
