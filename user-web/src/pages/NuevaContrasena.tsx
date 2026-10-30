import { useState } from 'react'
import { restablecerContrasena } from '../api/cuentas.ts'
import Formulario, { type Campo } from '../components/Formulario.tsx'
import { Acceso } from '../components/Pantalla.tsx'

// confirmacion solo existe en el front: al servidor se envía únicamente password.
const CAMPOS: Campo[] = [
  { nombre: 'password', etiqueta: 'Nueva contraseña', tipo: 'password' },
  { nombre: 'confirmacion', etiqueta: 'Repite la contraseña', tipo: 'password' },
]

function NuevaContrasena() {
  // uid y token vienen en el enlace del correo que arma admin-api.
  const parametros = new URLSearchParams(window.location.search)
  const uid = parametros.get('uid')
  const token = parametros.get('token')

  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [actualizada, setActualizada] = useState(false)
  const [enlaceInvalido, setEnlaceInvalido] = useState(!uid || !token)

  async function guardar(valores: Record<string, string>) {
    if (valores.password !== valores.confirmacion) {
      setErrores({ confirmacion: 'Las contraseñas no coinciden.' })
      return
    }
    setEnviando(true)
    setErrores({})
    try {
      // Sin uid o token no se muestra el formulario, así que aquí siempre existen.
      const { ok, cuerpo } = await restablecerContrasena({ uid: uid!, token: token!, password: valores.password })
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
    contenido = (
      <p>
        Contraseña actualizada. Ya puedes iniciar sesión. <a href="/login">Iniciar sesión</a>
      </p>
    )
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
