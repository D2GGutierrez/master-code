import { MAX_ALTERNATIVAS } from '../completitud.js'
import './Alternativas.css'

// Mientras el docente no toque la lista, Formulario no tiene valor: se parte de dos alternativas vacías.
const LISTA_INICIAL = [
  { id: 1, texto: '', es_correcta: false },
  { id: 2, texto: '', es_correcta: false },
]

// valor: lista de { id, texto, es_correcta }. id es solo de la pantalla (key estable) y no se envía.
// error: texto con el problema de la lista (p. ej. «Marca exactamente una…», lo pinta Formulario) o, si admin-api
// rechazó alguna alternativa, una lista con un objeto de errores por alternativa, en el orden de la lista.
function Alternativas({ id, etiqueta, valor, onCambio, error }) {
  const alternativas = valor || LISTA_INICIAL
  const errores = Array.isArray(error) ? error : []

  function cambiarTexto(idAlternativa, texto) {
    onCambio(alternativas.map((alt) => (alt.id === idAlternativa ? { ...alt, texto } : alt)))
  }

  // Exactamente una correcta: elegir una desmarca las demás.
  function marcarCorrecta(idAlternativa) {
    onCambio(alternativas.map((alt) => ({ ...alt, es_correcta: alt.id === idAlternativa })))
  }

  function agregar() {
    // El siguiente id sale de la lista y no de un contador: así no se repite aunque el componente se vuelva a montar.
    const siguiente = Math.max(0, ...alternativas.map((alt) => alt.id)) + 1
    onCambio([...alternativas, { id: siguiente, texto: '', es_correcta: false }])
  }

  function quitar(idAlternativa) {
    onCambio(alternativas.filter((alt) => alt.id !== idAlternativa))
  }

  return (
    <div id={id} role="group" aria-label={etiqueta} className="alternativas">
      <p className="campo-ayuda">
        Escribe entre 2 y {MAX_ALTERNATIVAS} alternativas y marca la correcta. El celular las mezcla en cada apertura.
      </p>
      {alternativas.map((alt, indice) => {
        const numero = indice + 1
        const idTexto = `alternativa-${alt.id}-texto`
        const mensaje = errores[indice]?.texto?.join(' ')
        return (
          <div key={alt.id} className="alternativa">
            <label className="alternativa-correcta">
              <input
                type="radio"
                name={`${id}-correcta`}
                checked={alt.es_correcta}
                onChange={() => marcarCorrecta(alt.id)}
                aria-label={`Marcar la alternativa ${numero} como correcta`}
              />
              Correcta
            </label>
            <div className="campo alternativa-texto">
              <label htmlFor={idTexto}>Alternativa {numero}</label>
              <input
                id={idTexto}
                type="text"
                value={alt.texto}
                onChange={(evento) => cambiarTexto(alt.id, evento.target.value)}
                aria-invalid={mensaje ? 'true' : undefined}
                aria-describedby={mensaje ? `${idTexto}-error` : undefined}
              />
              {mensaje && (
                <p id={`${idTexto}-error`} className="error" role="alert">
                  {mensaje}
                </p>
              )}
            </div>
            <button type="button" aria-label={`Quitar alternativa ${numero}`} onClick={() => quitar(alt.id)}>
              Quitar
            </button>
          </div>
        )
      })}
      <button type="button" onClick={agregar} disabled={alternativas.length >= MAX_ALTERNATIVAS}>
        Agregar alternativa
      </button>
    </div>
  )
}

export default Alternativas
