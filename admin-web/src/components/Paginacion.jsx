import './Paginacion.css'

function Paginacion({ pagina, hayAnterior, haySiguiente, onCambiar }) {
  return (
    <nav aria-label="Paginación">
      <button type="button" disabled={!hayAnterior} onClick={() => onCambiar(pagina - 1)}>
        Anterior
      </button>{' '}
      Página {pagina}{' '}
      <button type="button" disabled={!haySiguiente} onClick={() => onCambiar(pagina + 1)}>
        Siguiente
      </button>
    </nav>
  )
}

export default Paginacion
