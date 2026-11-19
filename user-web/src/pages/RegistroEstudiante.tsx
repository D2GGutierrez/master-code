import { useState } from 'react'
import { iniciarSesionConGoogle, registrarEstudiante } from '../api/cuentas.ts'
import { guardarSesion } from '../api/sesion.ts'
import BotonGoogle from '../components/BotonGoogle.tsx'
import Formulario, { type Campo } from '../components/Formulario.tsx'
import { Acceso } from '../components/Pantalla.tsx'

// Los nombres coinciden con los campos que espera admin-api.
const CAMPOS: Campo[] = [
  { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text' },
  { nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email', autoComplete: 'email' },
  {
    nombre: 'password',
    etiqueta: 'Contraseña',
    tipo: 'password',
    autoComplete: 'new-password',
    ayuda: 'Al menos 8 caracteres; no puede ser solo números ni una contraseña muy común.',
  },
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

  // Con Google no hay formulario: la primera vez que entra se crea la cuenta de estudiante.
  async function registrarConGoogle(credencial: string) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await iniciarSesionConGoogle(credencial)
      if (ok && cuerpo.access && cuerpo.refresh) {
        guardarSesion(cuerpo.access, cuerpo.refresh)
        window.location.assign('/inicio')
        return
      }
      setErrores({ general: cuerpo.detail ?? 'No se pudo continuar con Google. Inténtalo de nuevo.' })
    } catch {
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  return (
    <Acceso
      lema="Empieza a entrenar tu lógica"
      descripcion="Crea tu cuenta con tu correo institucional y únete a tus cursos con el código de tu docente."
      enlace={{ texto: '¿Ya tienes cuenta?', accion: 'Inicia sesión', href: '/login' }}
    >
      <h1>Crea tu cuenta de estudiante</h1>
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
      {!registrado && (
        <BotonGoogle
          texto="signup_with"
          alCredencial={registrarConGoogle}
          alFallar={() => setErrores({ general: 'No se pudo cargar el registro con Google. Revisa tu conexión.' })}
        />
      )}
      <p>
        <a href="/login">¿Ya tienes cuenta? Inicia sesión</a>
      </p>
    </Acceso>
  )
}

export default RegistroEstudiante
