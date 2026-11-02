// HU36-T3: el intento que será el próximo envío, a partir de los envíos previos del estudiante en el problema.
// Si no llegó una lista, no se sabe: null.
export function numeroDeIntento(envios: unknown): number | null {
  return Array.isArray(envios) ? envios.length + 1 : null
}
