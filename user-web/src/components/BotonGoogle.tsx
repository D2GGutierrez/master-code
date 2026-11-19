import { useEffect, useRef } from 'react'

// Lo mínimo que se usa de la biblioteca «Google Identity Services» (https://accounts.google.com/gsi/client).
type RespuestaGoogle = { credential?: string }
type ApiGoogle = {
  accounts: {
    id: {
      initialize: (opciones: { client_id: string; callback: (respuesta: RespuestaGoogle) => void; hd?: string }) => void
      renderButton: (elemento: HTMLElement, opciones: Record<string, string | number>) => void
    }
  }
}
declare global {
  interface Window {
    google?: ApiGoogle
  }
}

const SCRIPT_GOOGLE = 'https://accounts.google.com/gsi/client'
const ID_DE_CLIENTE: string = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''

// Carga la biblioteca de Google una sola vez, aunque haya varios botones o se vuelva a montar.
let carga: Promise<ApiGoogle> | undefined
function cargarGoogle() {
  carga ??= new Promise<ApiGoogle>((resolver, rechazar) => {
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

type Props = {
  // Recibe el token de identidad que firma Google; el servidor lo verifica.
  alCredencial: (credencial: string) => void
  // El texto del botón: «Registrarme con Google» o «Continuar con Google».
  texto?: 'signin_with' | 'signup_with' | 'continue_with'
  // Se avisa si la biblioteca de Google no se pudo cargar (sin conexión, bloqueador de anuncios...).
  alFallar?: () => void
}

// El botón oficial de Google. Sin VITE_GOOGLE_CLIENT_ID no se muestra: el inicio con contraseña sigue funcionando.
function BotonGoogle({ alCredencial, texto = 'continue_with', alFallar }: Props) {
  const contenedor = useRef<HTMLDivElement>(null)
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
