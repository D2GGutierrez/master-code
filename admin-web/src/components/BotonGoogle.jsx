import { useEffect, useRef } from 'react'

const SCRIPT_GOOGLE = 'https://accounts.google.com/gsi/client'
const ID_DE_CLIENTE = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''

// Carga la biblioteca de Google una sola vez, aunque haya varios botones o se vuelva a montar.
let carga
function cargarGoogle() {
  carga ??= new Promise((resolver, rechazar) => {
    if (window.google) {
      resolver(window.google)
      return
    }
    const script = document.createElement('script')
    script.src = SCRIPT_GOOGLE
    script.async = true
    script.onload = () => (window.google ? resolver(window.google) : rechazar(new Error('Google no respondió')))
    script.onerror = () => {
      // Se olvida la promesa fallida para poder reintentar (por ejemplo, tras volver la conexión).
      carga = undefined
      rechazar(new Error('No se pudo cargar Google'))
    }
    document.head.appendChild(script)
  })
  return carga
}

// El botón oficial de Google. Sin VITE_GOOGLE_CLIENT_ID no se muestra: el inicio con contraseña sigue funcionando.
// Props: alCredencial(credencial) recibe el token de identidad que firma Google (el servidor lo verifica);
// texto: 'signup_with' | 'continue_with' (el texto del botón); alFallar(): la biblioteca de Google no se pudo cargar.
function BotonGoogle({ alCredencial, texto = 'continue_with', alFallar }) {
  const contenedor = useRef(null)
  // La función de Google se registra una vez; con la ref siempre llama a la versión más reciente.
  const ultimo = useRef({ alCredencial, alFallar })
  ultimo.current = { alCredencial, alFallar }

  useEffect(() => {
    if (!ID_DE_CLIENTE) {
      return
    }
    let activo = true
    cargarGoogle()
      .then((google) => {
        if (!activo || !contenedor.current) {
          return
        }
        google.accounts.id.initialize({
          client_id: ID_DE_CLIENTE,
          callback: (respuesta) => {
            if (respuesta.credential) {
              ultimo.current.alCredencial(respuesta.credential)
            }
          },
        })
        google.accounts.id.renderButton(contenedor.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: texto,
          shape: 'rectangular',
          locale: 'es',
          width: 320,
        })
      })
      .catch(() => ultimo.current.alFallar?.())
    return () => {
      activo = false
    }
  }, [texto])

  if (!ID_DE_CLIENTE) {
    return null
  }
  return (
    <>
      <p className="acceso-separador" role="separator">
        <span>o</span>
      </p>
      <div className="acceso-google" ref={contenedor} />
    </>
  )
}

export default BotonGoogle
