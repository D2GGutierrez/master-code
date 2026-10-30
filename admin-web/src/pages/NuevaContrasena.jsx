import { useState } from 'react'
import { restablecerContrasena } from '../api/cuentas.js'
import Formulario from '../components/Formulario.jsx'
import { Acceso } from '../components/Pantalla.jsx'

// confirmacion solo existe en el front: al servidor se envía únicamente password.
const CAMPOS = [
  { nombre: 'password', etiqueta: 'Nueva contraseña', tipo: 'password' },
  { nombre: 'confirmacion', etiqueta: 'Repite la contraseña', tipo: 'password' },
]

function NuevaContrasena() {
  // uid y token vienen en el enlace del correo que arma admin-api.
  const parametros = new URLSearchParams(window.location.search)
  const uid = parametros.get('uid')
  const token = parametros.get('token')

  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})
  const [actualizada, setActualizada] = useState(false)
  const [enlaceInvalido, setEnlaceInvalido] = useState(!uid || !token)

  async function guardar(valores) {
    if (valores.password !== valores.confirmacion) {
      setErrores({ confirmacion: 'Las contraseñas no coinciden.' })
      return
    }
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await restablecerContrasena({ uid, token, password: valores.password })
      if (ok) {
        setActualizada(true)
      } else if (cuerpo.password) {
        setErrores({ password: cuerpo.password.join(' ') })
      } else if (cuerpo.detail) {
        // admin-api responde { detail } cuando el token es inválido, vencido o ya usado.
        setEnlaceInvalido(true)
      } else {
        setErrores({ general: 'No se pudo cambiar la contraseña. Inténtalo de nuevo.' })
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  let contenido
  if (actualizada) {
    contenido = <p>Contraseña actualizada. Ya puedes iniciar sesión.</p>
  } else if (enlaceInvalido) {
    contenido = (
      <p>
        El enlace no es válido o ya venció. <a href="/olvide-contrasena">Pide uno nuevo.</a>
      </p>
    )
  } else {
    contenido = (
      <Formulario
        campos={CAMPOS}
        textoBoton="Guardar contraseña"
        enviando={enviando}
        errores={errores}
        onEnviar={guardar}
      />
    )
  }

  return (
    <Acceso lema="Recupera tu acceso" descripcion="Elige una contraseña nueva para volver a entrar a tus cursos.">
      <h1>MASTER CODE · Nueva contraseña</h1>
      {contenido}
    </Acceso>
  )
}

export default NuevaContrasena
