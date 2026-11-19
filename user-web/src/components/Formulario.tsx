import { useState, type ChangeEvent, type FormEvent } from 'react'
import './Formulario.css'

export type Campo = {
  nombre: string
  etiqueta: string
  tipo: 'text' | 'email' | 'password'
  // Una línea de ayuda bajo el campo (qué se espera escribir).
  ayuda?: string
  autoComplete?: string
  // Para códigos numéricos: teclado de números en el móvil y largo máximo.
  inputMode?: 'numeric' | 'text'
  maxLength?: number
}

type Props = {
  campos: Campo[]
  textoBoton: string
  enviando: boolean
  // Un mensaje por nombre de campo y, opcionalmente, uno `general`.
  errores: Record<string, string>
  onEnviar: (valores: Record<string, string>) => void
}

function Formulario({ campos, textoBoton, enviando, errores, onEnviar }: Props) {
  const [valores, setValores] = useState<Record<string, string>>({})

  function manejarCambio(evento: ChangeEvent<HTMLInputElement>) {
    setValores({ ...valores, [evento.target.name]: evento.target.value })
  }

  function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
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
            autoComplete={campo.autoComplete}
            inputMode={campo.inputMode}
            maxLength={campo.maxLength}
            aria-describedby={campo.ayuda ? `${campo.nombre}-ayuda` : undefined}
            required
          />
          {campo.ayuda && (
            <p id={`${campo.nombre}-ayuda`} className="ayuda">
              {campo.ayuda}
            </p>
          )}
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
