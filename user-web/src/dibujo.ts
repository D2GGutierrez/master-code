// Ayudas para dibujar en SVG, compartidas por las ilustraciones y las portadas.

// Destello de cuatro puntas centrado en (x, y).
export function estrella(x: number, y: number, r: number) {
  const c = r * 0.3
  return (
    `M${x} ${y - r}C${x + 1} ${y - c} ${x + c} ${y - 1} ${x + r} ${y}C${x + c} ${y + 1} ${x + 1} ${y + c} ${x} ${y + r}` +
    `C${x - 1} ${y + c} ${x - c} ${y + 1} ${x - r} ${y}C${x - c} ${y - 1} ${x - 1} ${y - c} ${x} ${y - r}Z`
  )
}
