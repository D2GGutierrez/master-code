// HU62-T2: la selección de problemas de una evaluación, por id. Ninguna función modifica lo que recibe.

export function alternarSeleccion(ids, id) {
  return ids.includes(id) ? ids.filter((elegido) => elegido !== id) : [...ids, id]
}

// Sin acentos ni mayúsculas: «sumá» encuentra «Suma».
function comparable(texto) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
}

// El título se compara como texto: un «<b>» es parte del título, no una etiqueta.
export function filtrarPorTitulo(problemas, texto) {
  const buscado = comparable(texto).trim()
  if (!buscado) {
    return problemas
  }
  return problemas.filter((problema) => comparable(problema.titulo).includes(buscado))
}

// Los ids elegidos que siguen en el banco, en el orden del banco y sin repetir: así se envían al servidor.
export function ordenarComoBanco(problemas, ids) {
  const elegidos = new Set(ids)
  const vistos = new Set()
  const resultado = []
  for (const problema of problemas) {
    if (elegidos.has(problema.id) && !vistos.has(problema.id)) {
      vistos.add(problema.id)
      resultado.push(problema.id)
    }
  }
  return resultado
}

// Suma los ids visibles a la selección, sin quitar los que ya estaban ni repetir.
export function agregarVisibles(ids, visibles) {
  const resultado = [...ids]
  for (const id of visibles) {
    if (!resultado.includes(id)) {
      resultado.push(id)
    }
  }
  return resultado
}
