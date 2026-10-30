// Cada lenguaje tiene su paleta (clases leng-* de diseno.css). Lo que no se conoce usa la paleta «otro».
// Los valores son los que admin-api acepta en cursos y problemas.
const CLAVE_POR_LENGUAJE = new Map([
  ['python', 'python'],
  ['java', 'java'],
  ['javascript', 'javascript'],
])

export function claveLenguaje(lenguaje) {
  return CLAVE_POR_LENGUAJE.get(String(lenguaje).trim().toLowerCase()) ?? 'otro'
}
