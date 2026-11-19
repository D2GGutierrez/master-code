import { useState } from 'react'
import { iniciarSesionConGoogle, registrarDocente } from '../api/cuentas.js'
import { tomarRutaGuardada } from '../api/sesion.js'
import BotonGoogle from '../components/BotonGoogle.jsx'
import Formulario from '../components/Formulario.jsx'
import { Acceso } from '../components/Pantalla.jsx'

// Los nombres coinciden con los campos que espera admin-api.
const CAMPOS = [
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
function erroresDelServidor(cuerpo) {
  const errores = {}
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

function RegistroDocente() {
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})

  async function registrar(valores) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await registrarDocente(valores)
      if (ok) {
        // admin-api acaba de enviar el código al correo: se pide en la pantalla siguiente.
        window.location.assign(`/verificar-correo?email=${encodeURIComponent(valores.email.trim().toLowerCase())}`)
        // El botón sigue deshabilitado mientras el navegador cambia de página.
        return
      } else {
        setErrores(erroresDelServidor(cuerpo))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  // Con Google, un docente que está en la lista de autorizados entra de inmediato, sin esperar al Product Owner.
  async function entrarConGoogle(credencial) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await iniciarSesionConGoogle(credencial)
      if (ok) {
        window.location.assign(tomarRutaGuardada())
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
      lema="Crea problemas en minutos"
      descripcion="Regístrate con tu correo institucional y confírmalo con el código que te enviamos. Tu cuenta se activa cuando el Product Owner la apruebe."
      enlace={{ texto: '¿Ya tienes cuenta?', accion: 'Inicia sesión', href: '/login' }}
    >
      <h1>Crea tu cuenta de docente</h1>
      <Formulario
        campos={CAMPOS}
        textoBoton="Registrarme"
        enviando={enviando}
        errores={errores}
        onEnviar={registrar}
      />
      <BotonGoogle
        texto="signup_with"
        alCredencial={entrarConGoogle}
        alFallar={() => setErrores({ general: 'No se pudo cargar el registro con Google. Revisa tu conexión.' })}
      />
    </Acceso>
  )
}

export default RegistroDocente
