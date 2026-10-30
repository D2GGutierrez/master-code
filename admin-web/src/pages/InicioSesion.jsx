import { useState } from 'react'
import { iniciarSesion } from '../api/cuentas.js'
import { hayRutaGuardada, tomarRutaGuardada } from '../api/sesion.js'
import Formulario from '../components/Formulario.jsx'
import { Acceso } from '../components/Pantalla.jsx'

// Los nombres coinciden con los campos que espera admin-api.
const CAMPOS = [
  { nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email' },
  { nombre: 'password', etiqueta: 'Contraseña', tipo: 'password' },
]

// admin-api responde { detail } (credenciales incorrectas, cuenta inactiva) o { campo: ['mensaje', ...] }.
function erroresDelServidor(cuerpo) {
  const errores = {}
  for (const campo of CAMPOS) {
    if (cuerpo[campo.nombre]) {
      errores[campo.nombre] = cuerpo[campo.nombre].join(' ')
    }
  }
  if (cuerpo.detail) {
    errores.general = cuerpo.detail
  }
  if (Object.keys(errores).length === 0) {
    errores.general = 'No se pudo iniciar sesión. Inténtalo de nuevo.'
  }
  return errores
}

function InicioSesion() {
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})

  async function entrar(valores) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await iniciarSesion(valores)
      if (ok) {
        // El botón queda deshabilitado mientras el navegador cambia de página.
        window.location.assign(tomarRutaGuardada())
        return
      }
      setErrores(erroresDelServidor(cuerpo))
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  return (
    <Acceso lema="Tus cursos, con ayuda de la IA" descripcion="Crea cursos, genera problemas con inteligencia artificial y sigue el avance de tus estudiantes.">
      <h1>MASTER CODE · Inicio de sesión</h1>
      {/* Solo hay ruta guardada si se llegó aquí porque la sesión expiró. */}
      {hayRutaGuardada() && <p>Tu sesión expiró. Inicia sesión para continuar.</p>}
      <Formulario
        campos={CAMPOS}
        textoBoton="Iniciar sesión"
        enviando={enviando}
        errores={errores}
        onEnviar={entrar}
      />
      <p>
        <a href="/olvide-contrasena">¿Olvidaste tu contraseña?</a>
      </p>
    </Acceso>
  )
}

export default InicioSesion
