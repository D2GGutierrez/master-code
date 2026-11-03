// HU44-T2: porcentaje (0 a 100, entero) de envíos correctos sobre envíos totales. null si no se puede calcular: sin
// envíos o datos que no son números, para no mostrar nunca NaN, Infinity ni dividir entre cero.
export function porcentajeAcierto(correctos, intentos) {
  if (typeof correctos !== 'number' || typeof intentos !== 'number') {
    return null
  }
  if (!Number.isFinite(correctos) || !Number.isFinite(intentos) || intentos <= 0) {
    return null
  }
  const acotados = Math.min(Math.max(correctos, 0), intentos)
  return Math.round((acotados / intentos) * 100)
}
