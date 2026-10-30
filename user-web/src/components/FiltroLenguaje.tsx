import { claveLenguaje, etiquetaLenguaje, type ClaveLenguaje } from '../lenguajes.ts'

const ORDEN: ClaveLenguaje[] = ['python', 'java', 'javascript', 'otro']

// «Otro» agrupa los lenguajes que la pantalla no conoce.
function etiquetaClave(clave: ClaveLenguaje) {
  return clave === 'otro' ? 'Otros' : etiquetaLenguaje(clave)
}

// Filtro por lenguaje en forma de píldoras, con cuántos cursos hay de cada uno. Solo aparece si hay al menos dos
// lenguajes distintos: con uno solo no hay nada que filtrar.
export function FiltroLenguaje({
  cursos,
  valor,
  onCambio,
}: {
  cursos: { lenguaje: string }[]
  valor: ClaveLenguaje | ''
  onCambio: (nuevo: ClaveLenguaje | '') => void
}) {
  const opciones = ORDEN.map((clave) => ({
    clave,
    cantidad: cursos.filter((curso) => claveLenguaje(curso.lenguaje) === clave).length,
  })).filter((opcion) => opcion.cantidad > 0)
  if (opciones.length < 2) {
    return null
  }
  return (
    <div className="chips" role="group" aria-label="Filtrar por lenguaje">
      <button type="button" className="chip chip-todos" aria-pressed={valor === ''} onClick={() => onCambio('')}>
        Todos <small>{cursos.length}</small>
      </button>
      {opciones.map(({ clave, cantidad }) => (
        <button
          key={clave}
          type="button"
          className={`chip leng-${clave}`}
          aria-pressed={valor === clave}
          onClick={() => onCambio(clave)}
        >
          {etiquetaClave(clave)} <small>{cantidad}</small>
        </button>
      ))}
    </div>
  )
}
