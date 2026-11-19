import { useEffect, useState } from 'react'
import { reenviarCodigo, verificarCorreo } from '../api/cuentas.ts'
import Formulario, { type Campo } from '../components/Formulario.tsx'
import { Acceso } from '../components/Pantalla.tsx'

// Segundos entre un código y el siguiente (el mismo límite que pone admin-api).
const ESPERA_SEGUNDOS = 60

const CAMPO_CODIGO: Campo = {
  nombre: 'codigo',
  etiqueta: 'Código de 6 dígitos',
  tipo: 'text',
  autoComplete: 'one-time-code',
  inputMode: 'numeric',
  maxLength: 6,
}
const CAMPO_CORREO: Campo = { nombre: 'email', etiqueta: 'Correo institucional', tipo: 'email', autoComplete: 'username' }

// Se llega aquí tras registrarse (?email=...) o desde el inicio de sesión si falta confirmar el correo.
function VerificarCorreo() {
  const correoDeLaUrl = new URLSearchParams(window.location.search).get('email')?.trim().toLowerCase() ?? ''
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [aviso, setAviso] = useState('')
  const [listo, setListo] = useState('')
  // El código se acaba de enviar al registrarse: el reenvío se habilita después de un minuto.
  const [espera, setEspera] = useState(correoDeLaUrl ? ESPERA_SEGUNDOS : 0)
  const [correoEscrito, setCorreoEscrito] = useState('')

  useEffect(() => {
    if (espera <= 0) {
      return
    }
    const temporizador = setTimeout(() => setEspera((segundos) => segundos - 1), 1000)
    return () => clearTimeout(temporizador)
  }, [espera])

  const correo = correoDeLaUrl || correoEscrito

  async function verificar(valores: Record<string, string>) {
    const email = (correoDeLaUrl || valores.email || '').trim().toLowerCase()
    setCorreoEscrito(email)
    setEnviando(true)
    setErrores({})
    setAviso('')
    try {
      const { ok, cuerpo } = await verificarCorreo(email, (valores.codigo ?? '').trim())
      if (ok) {
        setListo(cuerpo.detail ?? 'Correo verificado.')
      } else {
        setErrores({ general: cuerpo.detail ?? 'No se pudo verificar el código. Inténtalo de nuevo.' })
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  async function pedirOtroCodigo() {
    if (!correo || espera > 0) {
      setErrores({ general: 'Escribe tu correo para pedir un código nuevo.' })
      return
    }
    setErrores({})
    setAviso('')
    try {
      const { ok, cuerpo } = await reenviarCodigo(correo)
      if (ok) {
        setAviso('Te enviamos un código nuevo. El anterior ya no sirve.')
        setEspera(ESPERA_SEGUNDOS)
      } else {
        setErrores({ general: cuerpo.detail ?? 'No se pudo enviar el código. Inténtalo de nuevo.' })
      }
    } catch {
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
  }

  return (
    <Acceso
      lema="Confirma tu correo institucional"
      descripcion="Te enviamos un código de 6 dígitos para comprobar que el correo es tuyo."
      enlace={{ texto: '¿Ya lo confirmaste?', accion: 'Inicia sesión', href: '/login' }}
    >
      <h1>Confirma tu correo</h1>
      {listo ? (
        <>
          <p>{listo}</p>
          <p>
            <a href="/login">Ir a iniciar sesión</a>
          </p>
        </>
      ) : (
        <>
          {correoDeLaUrl && (
            <p>
              Te enviamos un código a <strong>{correoDeLaUrl}</strong>. Revisa tu bandeja de entrada y la carpeta de spam.
            </p>
          )}
          <Formulario
            campos={correoDeLaUrl ? [CAMPO_CODIGO] : [CAMPO_CORREO, CAMPO_CODIGO]}
            textoBoton="Confirmar correo"
            enviando={enviando}
            errores={errores}
            onEnviar={verificar}
          />
          {aviso && <p role="status">{aviso}</p>}
          <p>
            <button type="button" className="enlace" onClick={pedirOtroCodigo} disabled={espera > 0}>
              {espera > 0 ? `Pedir otro código (${espera} s)` : 'Pedir otro código'}
            </button>
          </p>
        </>
      )}
    </Acceso>
  )
}

export default VerificarCorreo
