// El nombre con el que se saluda al estudiante sale de su correo: la sesión no trae el nombre, solo el correo.

// «ana.torres@tecsup.edu.pe» → «Ana». Si el inicio del correo no parece un nombre (un código como «u202112345» o
// letras sin vocales como «jdgdd»), devuelve null y la pantalla saluda sin nombre.
export function nombreDesdeCorreo(correo: string | null | undefined): string | null {
  const local = (correo ?? '').split('@')[0]
  const primera = local.split(/[._+-]/)[0]
  if (!/^\p{L}{2,20}$/u.test(primera) || !/[aeiouáéíóúü]/i.test(primera)) {
    return null
  }
  return primera.charAt(0).toUpperCase() + primera.slice(1).toLowerCase()
}

// La letra del círculo de la cabecera.
export function inicialDeCorreo(correo: string | null | undefined): string {
  const primera = (correo ?? '').trim().charAt(0)
  return primera ? primera.toUpperCase() : '?'
}
