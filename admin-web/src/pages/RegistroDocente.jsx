import { useState } from 'react'
import { registrarDocente } from '../api/cuentas.js'
import Formulario from '../components/Formulario.jsx'
import { Acceso } from '../components/Pantalla.jsx'

// Los nombres coinciden con los campos que espera admin-api.
const CAMPOS = [
  { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text' },
  { nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email' },
  { nombre: 'password', etiqueta: 'Contraseña', tipo: 'password' },
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
  const [registrado, setRegistrado] = useState(false)

  async function registrar(valores) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await registrarDocente(valores)
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
    <Acceso lema="Crea problemas en minutos" descripcion="Regístrate con tu correo institucional. Tu cuenta se activa cuando el Product Owner la apruebe.">
      <h1>MASTER CODE · Registro del docente</h1>
      {registrado ? (
        <p>Registro recibido. Tu cuenta quedará pendiente hasta que el Product Owner la active.</p>
      ) : (
        <Formulario
          campos={CAMPOS}
          textoBoton="Registrarme"
          enviando={enviando}
          errores={errores}
          onEnviar={registrar}
        />
      )}
    </Acceso>
  )
}

export default RegistroDocente
