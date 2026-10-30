// Valores de lenguaje que admin-api acepta en cursos y problemas (LENGUAJES en cursos/serializers.py).
// Map y no objeto: así un valor como "constructor" no encuentra propiedades heredadas.
const MONACO_POR_LENGUAJE = new Map([
  ['python', 'python'],
  ['java', 'java'],
  ['javascript', 'javascript'],
])

export function idMonaco(lenguaje: string): string {
  return MONACO_POR_LENGUAJE.get(lenguaje.trim().toLowerCase()) ?? 'plaintext'
}

const ETIQUETA_POR_LENGUAJE = new Map([
  ['python', 'Python'],
  ['java', 'Java'],
  ['javascript', 'JavaScript'],
])

// Un valor desconocido se muestra tal cual llegó.
export function etiquetaLenguaje(lenguaje: string): string {
  return ETIQUETA_POR_LENGUAJE.get(lenguaje.trim().toLowerCase()) ?? lenguaje
}

// Cada lenguaje tiene su paleta (clases leng-* de diseno.css). Lo que no se conoce usa la paleta «otro».
export type ClaveLenguaje = 'python' | 'java' | 'javascript' | 'otro'

const CLAVE_POR_LENGUAJE = new Map<string, ClaveLenguaje>([
  ['python', 'python'],
  ['java', 'java'],
  ['javascript', 'javascript'],
])

export function claveLenguaje(lenguaje: string): ClaveLenguaje {
  return CLAVE_POR_LENGUAJE.get(lenguaje.trim().toLowerCase()) ?? 'otro'
}
