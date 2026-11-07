// HU44-T3: razón (0 a 1, exacta) de envíos correctos sobre envíos totales. null si no se puede calcular: sin envíos o
// datos que no son números, para no mostrar nunca NaN, Infinity ni dividir entre cero.
export function razonAcierto(correctos, intentos) {
  if (typeof correctos !== 'number' || typeof intentos !== 'number') {
    return null
  }
  if (!Number.isFinite(correctos) || !Number.isFinite(intentos) || intentos <= 0) {
    return null
  }
  const acotados = Math.min(Math.max(correctos, 0), intentos)
  return acotados / intentos
}

// HU44-T2: el mismo acierto como porcentaje entero (0 a 100), o null.
export function porcentajeAcierto(correctos, intentos) {
  const razon = razonAcierto(correctos, intentos)
  return razon === null ? null : Math.round(razon * 100)
}

// HU44-T3 (CA-044.3): una copia de las filas ordenada por la razón exacta, no por el porcentaje redondeado. 'asc' pone
// primero el menor acierto y 'desc' el mayor; con otro valor, la copia queda en el orden recibido. Sin razón (sin
// envíos o datos inválidos) siempre al final. Empates: más envíos primero, luego el título; sin título, al final.
export function ordenarPorAcierto(filas, direccion) {
  const copia = [...filas]
  if (direccion !== 'asc' && direccion !== 'desc') {
    return copia
  }
  const signo = direccion === 'asc' ? 1 : -1
  return copia.sort((a, b) => {
    const razonA = razonAcierto(a.correctos, a.intentos)
    const razonB = razonAcierto(b.correctos, b.intentos)
    if (razonA === null || razonB === null) {
      return (razonA === null) - (razonB === null)
    }
    if (razonA !== razonB) {
      return (razonA - razonB) * signo
    }
    if (a.intentos !== b.intentos) {
      return b.intentos - a.intentos
    }
    if (typeof a.titulo !== 'string' || typeof b.titulo !== 'string') {
      return (typeof a.titulo !== 'string') - (typeof b.titulo !== 'string')
    }
    return a.titulo.localeCompare(b.titulo, 'es')
  })
}
