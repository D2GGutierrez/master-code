// HU41-T3: paginación en el navegador, sobre una lista que ya llegó completa. No modifica la lista.
export function paginar<T>(lista: T[], pagina: number, tamano: number) {
  const total = lista.length
  const totalPaginas = Math.max(1, Math.ceil(total / tamano))
  // Una página fuera de rango (o que no es un número) cae a la más cercana válida.
  const actual = Number.isFinite(pagina) ? Math.min(Math.max(1, Math.trunc(pagina)), totalPaginas) : 1
  const inicio = (actual - 1) * tamano
  const items = lista.slice(inicio, inicio + tamano)
  return {
    items,
    pagina: actual,
    totalPaginas,
    // Posiciones desde 1 del rango que se muestra; 0 y 0 si no hay nada.
    desde: total === 0 ? 0 : inicio + 1,
    hasta: inicio + items.length,
    total,
  }
}

// El parámetro «pagina» de la URL (?pagina=2): solo un entero desde 1; cualquier otra cosa es la página 1.
export function leerPagina(search: string): number {
  const valor = new URLSearchParams(search).get('pagina')
  return valor !== null && /^[1-9]\d*$/.test(valor) ? Number(valor) : 1
}
