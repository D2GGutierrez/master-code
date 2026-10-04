import { useState } from 'react'
import './Formulario.css'

function Formulario({ campos, textoBoton, enviando, errores, onEnviar }) {
  const [valores, setValores] = useState({})

  function manejarCambio(evento) {
    setValores({ ...valores, [evento.target.name]: evento.target.value })
  }

  function manejarEnvio(evento) {
    // Evita que el navegador recargue la página al enviar.
    evento.preventDefault()
    onEnviar(valores)
  }

  return (
    <form className="formulario" onSubmit={manejarEnvio}>
      {errores.general && <p className="error">{errores.general}</p>}
      {campos.map((campo) => (
        <div key={campo.nombre} className="campo">
          <label htmlFor={campo.nombre}>{campo.etiqueta}</label>
          <input
            id={campo.nombre}
            name={campo.nombre}
            type={campo.tipo}
            value={valores[campo.nombre] ?? ''}
            onChange={manejarCambio}
            required
          />
          {errores[campo.nombre] && <p className="error">{errores[campo.nombre]}</p>}
        </div>
      ))}
      <button type="submit" disabled={enviando}>
        {textoBoton}
      </button>
    </form>
  )
}

export default Formulario
