import { useState } from 'react'
import { iniciarSesion, type CuerpoSesion } from '../api/cuentas.ts'
import { guardarSesion } from '../api/sesion.ts'
import Formulario, { type Campo } from '../components/Formulario.tsx'

// Los nombres coinciden con los campos que espera admin-api.
const CAMPOS: Campo[] = [
  { nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email' },
  { nombre: 'password', etiqueta: 'Contraseña', tipo: 'password' },
]

// admin-api responde { detail } (credenciales incorrectas, cuenta inactiva) o { campo: ['mensaje', ...] }.
function erroresDelServidor(cuerpo: CuerpoSesion) {
  const errores: Record<string, string> = {}
  if (cuerpo.email) {
    errores.email = cuerpo.email.join(' ')
  }
  if (cuerpo.password) {
    errores.password = cuerpo.password.join(' ')
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
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [iniciada, setIniciada] = useState(false)

  async function entrar(valores: Record<string, string>) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await iniciarSesion(valores)
      if (ok && cuerpo.access && cuerpo.refresh) {
        guardarSesion(cuerpo.access, cuerpo.refresh)
        setIniciada(true)
      } else {
        setErrores(erroresDelServidor(cuerpo))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  return (
    <main>
      <h1>Inicio de sesión</h1>
      {iniciada ? (
        <p>Sesión iniciada.</p>
      ) : (
        <>
          <Formulario
            campos={CAMPOS}
            textoBoton="Iniciar sesión"
            enviando={enviando}
            errores={errores}
            onEnviar={entrar}
          />
          <p>
            <a href="/">¿No tienes cuenta? Regístrate</a>
          </p>
        </>
      )}
    </main>
  )
}

export default InicioSesion
