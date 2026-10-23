// Un texto que empieza así se ejecuta como fórmula al abrir el CSV en Excel o Sheets.
const INICIO_DE_FORMULA = /^[=+\-@\t\r]/

// RFC 4180. Los datos vienen de los estudiantes (no son de confianza): se neutralizan las fórmulas
// con una comilla simple antes de escapar. Los encabezados son nuestros y no se tocan.
function celda(valor, esDato) {
  if (valor === null || valor === undefined) {
    return ''
  }
  let texto = String(valor)
  if (esDato && INICIO_DE_FORMULA.test(texto)) {
    texto = `'${texto}`
  }
  return /[",\r\n]/.test(texto) ? `"${texto.replaceAll('"', '""')}"` : texto
}

// columnas: [{ encabezado, valor: (fila) => ... }]. El BOM hace que Excel lea bien tildes y eñes.
export function aCsv(columnas, filas) {
  const lineas = [columnas.map((columna) => celda(columna.encabezado, false)).join(',')]
  for (const fila of filas) {
    lineas.push(columnas.map((columna) => celda(columna.valor(fila), true)).join(','))
  }
  return `﻿${lineas.join('\r\n')}`
}

export function descargarCsv(nombreArchivo, contenido) {
  const url = URL.createObjectURL(new Blob([contenido], { type: 'text/csv;charset=utf-8' }))
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}

// «Programación 1 – Sección A» → inscritos-programacion-1-seccion-a-2026-10-27.csv
export function nombreArchivoInscritos(nombreCurso, id) {
  const hoy = new Date()
  const fecha = [hoy.getFullYear(), String(hoy.getMonth() + 1).padStart(2, '0'), String(hoy.getDate()).padStart(2, '0')].join('-')
  const curso = nombreCurso
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40)
    // El recorte puede dejar un guion al final.
    .replace(/-+$/, '')
  return curso ? `inscritos-${curso}-${fecha}.csv` : `inscritos-curso-${id}-${fecha}.csv`
}
