import { limpiarEnunciado } from '../enunciado.ts'

// Los estilos (.enunciado, .enunciado-texto) siguen en DetalleProblema.css.
function Enunciado({ texto }: { texto: string }) {
  const enunciado = limpiarEnunciado(texto)
  return 'html' in enunciado ? (
    // Único HTML insertado en user-web: ya pasó por DOMPurify en limpiarEnunciado.
    <div className="enunciado" dangerouslySetInnerHTML={{ __html: enunciado.html }} />
  ) : (
    <p className="enunciado enunciado-texto">{enunciado.texto}</p>
  )
}

export default Enunciado
