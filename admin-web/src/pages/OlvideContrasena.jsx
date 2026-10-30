import { useState } from 'react'
import { solicitarRecuperacion } from '../api/cuentas.js'
import Formulario from '../components/Formulario.jsx'
import { Acceso } from '../components/Pantalla.jsx'

const CAMPOS = [{ nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email' }]

function OlvideContrasena() {
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})
  const [enviado, setEnviado] = useState(false)

  async function solicitar(valores) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await solicitarRecuperacion(valores.email)
      if (ok) {
        setEnviado(true)
      } else if (cuerpo.email) {
        setErrores({ email: cuerpo.email.join(' ') })
      } else {
        setErrores({ general: 'No se pudo enviar la solicitud. Inténtalo de nuevo.' })
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  return (
    <Acceso lema="Recupera tu acceso" descripcion="Te enviamos un enlace al correo para que elijas una contraseña nueva.">
      <h1>MASTER CODE · Olvidé mi contraseña</h1>
      {enviado ? (
        // Mensaje genérico: no revela si el correo está registrado.
        <p>Si el correo está registrado, te enviamos un enlace para cambiar tu contraseña.</p>
      ) : (
        <Formulario
          campos={CAMPOS}
          textoBoton="Enviar enlace"
          enviando={enviando}
          errores={errores}
          onEnviar={solicitar}
        />
      )}
    </Acceso>
  )
}

export default OlvideContrasena
