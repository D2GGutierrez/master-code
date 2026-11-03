// HU34-T2: el código base va exacto, con su sangría y sus saltos de línea. Solo un valor que no es texto o que es
// únicamente espacios y saltos de línea se trata como vacío.
export function normalizarCodigoBase(valor) {
  if (typeof valor !== 'string' || !valor.trim()) {
    return ''
  }
  return valor
}
