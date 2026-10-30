// Marcadores de posición con brillo mientras llegan los cursos: la pantalla tiene su forma desde el primer momento.
// Son decorativos; el aviso de «cargando» va en texto, solo para lectores de pantalla.
export function EsqueletoCursos({ cantidad = 3, aviso }: { cantidad?: number; aviso: string }) {
  return (
    <div role="status">
      <span className="solo-lectores">{aviso}</span>
      <ul className="rejilla-cursos" aria-hidden="true">
        {Array.from({ length: cantidad }, (_, indice) => (
          <li key={indice} className="esqueleto-tarjeta">
            <div className="esqueleto" />
            <div className="esqueleto" />
            <div className="esqueleto" />
            <div className="esqueleto" />
          </li>
        ))}
      </ul>
    </div>
  )
}
