import './Paginacion.css'

type Props = {
  pagina: number
  totalPaginas: number
  desde: number
  hasta: number
  total: number
  // Lo que se cuenta en el anuncio: «envíos», «problemas»…
  etiqueta?: string
  alCambiar: (pagina: number) => void
}

// HU41-T3: Anterior / Siguiente para una lista paginada en el navegador (paginar, en paginacion.ts). En los extremos
// el botón usa aria-disabled y no disabled: así no pierde el foco quien llega a la última página con el teclado.
function Paginacion({ pagina, totalPaginas, desde, hasta, total, etiqueta = 'elementos', alCambiar }: Props) {
  if (totalPaginas <= 1) {
    return null
  }
  const esPrimera = pagina <= 1
  const esUltima = pagina >= totalPaginas
  return (
    <nav className="paginacion" aria-label="Paginación">
      <button
        type="button"
        aria-disabled={esPrimera ? 'true' : undefined}
        onClick={() => !esPrimera && alCambiar(pagina - 1)}
      >
        Anterior
      </button>
      <span>
        Página {pagina} de {totalPaginas}
      </span>
      <button
        type="button"
        aria-disabled={esUltima ? 'true' : undefined}
        onClick={() => !esUltima && alCambiar(pagina + 1)}
      >
        Siguiente
      </button>
      <p className="paginacion-rango" aria-live="polite">
        Mostrando {desde}–{hasta} de {total} {etiqueta}
      </p>
    </nav>
  )
}

export default Paginacion
