import { useState } from 'react'
import { registrarEstudiante } from '../api/cuentas.ts'
import Formulario, { type Campo } from '../components/Formulario.tsx'

// Los nombres coinciden con los campos que espera admin-api.
const CAMPOS: Campo[] = [
  { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text' },
  { nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email' },
  { nombre: 'password', etiqueta: 'Contraseña', tipo: 'password' },
]

// admin-api responde los errores como { campo: ['mensaje', ...] }.
function erroresDelServidor(cuerpo: Record<string, string[]>) {
  const errores: Record<string, string> = {}
  for (const campo of CAMPOS) {
    if (cuerpo[campo.nombre]) {
      errores[campo.nombre] = cuerpo[campo.nombre].join(' ')
    }
  }
  if (Object.keys(errores).length === 0) {
    errores.general = 'No se pudo completar el registro. Inténtalo de nuevo.'
  }
  return errores
}

function RegistroEstudiante() {
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [registrado, setRegistrado] = useState(false)

  async function registrar(valores: Record<string, string>) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await registrarEstudiante(valores)
      if (ok) {
        setRegistrado(true)
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
      <h1>Registro de estudiante</h1>
      {registrado ? (
        <p>Cuenta creada. Ya puedes iniciar sesión.</p>
      ) : (
        <Formulario
          campos={CAMPOS}
          textoBoton="Registrarme"
          enviando={enviando}
          errores={errores}
          onEnviar={registrar}
        />
      )}
      <p>
        <a href="/login">¿Ya tienes cuenta? Inicia sesión</a>
      </p>
    </main>
  )
}

export default RegistroEstudiante
