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
