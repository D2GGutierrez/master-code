// H3: la respuesta con la que se corrige un ejercicio del celular (D15). Es opcional para el servidor mientras se
// guarda un borrador, pero un problema sin ella no se puede aprobar, por eso el formulario la exige.
function CampoRespuesta({ id, valor, error, onCambio, ayuda, filas }) {
  const idAyuda = `${id}-ayuda`
  return (
    <>
      <textarea
        id={id}
        name={id}
        className="campo-codigo"
        rows={filas}
        spellCheck={false}
        autoComplete="off"
        autoCapitalize="off"
        value={valor}
        onChange={(evento) => onCambio(evento.target.value)}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${idAyuda} ${id}-error` : idAyuda}
      />
      <p id={idAyuda} className="campo-ayuda">
        {ayuda}
      </p>
    </>
  )
}

// Predicción de salida: lo que imprime el código del enunciado.
export function RespuestaSalida(props) {
  return (
    <CampoRespuesta
      {...props}
      filas={3}
      ayuda="Escribe exactamente lo que imprime el código del enunciado. Al corregir se ignoran las mayúsculas y los espacios de más; los signos sí cuentan."
    />
  )
}

// Completar el espacio: el texto que va en el hueco ___ del enunciado.
export function RespuestaHueco(props) {
  return (
    <CampoRespuesta
      {...props}
      filas={2}
      ayuda="Escribe solo lo que va en el hueco, no el código completo. El hueco se marca en el enunciado con ___ (tres guiones bajos), dentro de un bloque de código y una sola vez. Al corregir se ignoran las mayúsculas y los espacios de más."
    />
  )
}
