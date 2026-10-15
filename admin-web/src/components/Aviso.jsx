import './Aviso.css'

// Mensaje informativo (no de error): role="status" lo anuncia sin interrumpir al lector de pantalla.
function Aviso({ mensaje }) {
  if (!mensaje) {
    return null
  }
  return (
    <div role="status" className="aviso">
      {mensaje}
    </div>
  )
}

export default Aviso
