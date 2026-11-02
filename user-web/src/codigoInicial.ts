// HU34-T3 (decisión D8): al abrir el editor gana el borrador guardado, luego el código base y si no, vacío.
// Un borrador vacío también gana: el estudiante pudo borrarlo todo. El código base va exacto, con su sangría.
export function codigoInicial(codigoBase: string | null | undefined, borrador?: string | null): string {
  if (typeof borrador === 'string') {
    return borrador
  }
  if (typeof codigoBase === 'string') {
    return codigoBase
  }
  return ''
}
