import { describe, expect, it } from 'vitest'
import { inicialDeCorreo, nombreDesdeCorreo } from './persona.ts'

describe('nombreDesdeCorreo', () => {
  it('toma la primera palabra del correo, con la inicial en mayúscula', () => {
    expect(nombreDesdeCorreo('ana.torres.demo@tecsup.edu.pe')).toBe('Ana')
    expect(nombreDesdeCorreo('JUAN_PEREZ@tecsup.edu.pe')).toBe('Juan')
    expect(nombreDesdeCorreo('maría-lópez@tecsup.edu.pe')).toBe('María')
  })

  it('no inventa un nombre cuando el correo no lo trae', () => {
    // Un código de alumno, letras sin vocales, una sola letra o nada.
    expect(nombreDesdeCorreo('u202112345@tecsup.edu.pe')).toBeNull()
    expect(nombreDesdeCorreo('jdgdd@tecsup.edu.pe')).toBeNull()
    expect(nombreDesdeCorreo('a@tecsup.edu.pe')).toBeNull()
    expect(nombreDesdeCorreo('')).toBeNull()
    expect(nombreDesdeCorreo(null)).toBeNull()
    expect(nombreDesdeCorreo(undefined)).toBeNull()
  })
})

describe('inicialDeCorreo', () => {
  it('es la primera letra en mayúscula, o un signo si no hay correo', () => {
    expect(inicialDeCorreo('ana@tecsup.edu.pe')).toBe('A')
    expect(inicialDeCorreo('')).toBe('?')
    expect(inicialDeCorreo(null)).toBe('?')
  })
})
