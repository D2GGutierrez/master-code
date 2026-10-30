import { useEffect, useState } from 'react'
import { inscribirse, type Inscripcion } from '../api/cursos.ts'
import Formulario, { type Campo } from '../components/Formulario.tsx'
import { etiquetaLenguaje } from '../lenguajes.ts'
import { EstadoVacio, Hero } from '../components/Pantalla.tsx'

const CAMPOS: Campo[] = [{ nombre: 'codigo', etiqueta: 'Código de invitación', tipo: 'text' }]

// HU26: el estudiante escribe el código que le dio su docente. user-api lo valida con admin-api.
function Inscribirse() {
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [inscrita, setInscrita] = useState<Inscripcion | null>(null)

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
    }
  }, [])

  async function inscribir(valores: Record<string, string>) {
    setEnviando(true)
    setErrores({})
    try {
      const { ok, cuerpo } = await inscribirse(valores.codigo ?? '')
      if (ok) {
        setInscrita(cuerpo as Inscripcion)
      } else {
        // El servidor explica la causa: código incorrecto, vencido, ya inscrito o servicio caído.
        setErrores({ general: cuerpo.detail ?? 'No se pudo completar la inscripción. Inténtalo de nuevo.' })
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.' })
    }
    setEnviando(false)
  }

  return (
    <main className="inscribirse">
      <Hero clase="hero-compacto">
        <p className="hero-sobretitulo">Nuevo curso</p>
        <h1>Inscribirme en un curso</h1>
        {!inscrita && <p>Escribe el código de invitación que te dio tu docente.</p>}
      </Hero>
      {inscrita ? (
        <EstadoVacio ilustracion="exito">
          <p role="status">
            Te inscribiste en “{inscrita.nombre}” ({etiquetaLenguaje(inscrita.lenguaje)}).
          </p>
          <p>
            <a href={`/cursos/${inscrita.curso_id}/problemas`}>Ver los problemas del curso</a>
          </p>
        </EstadoVacio>
      ) : (
        <div className="panel-formulario">
          <Formulario
            campos={CAMPOS}
            textoBoton="Inscribirme"
            enviando={enviando}
            errores={errores}
            onEnviar={inscribir}
          />
        </div>
      )}
      <p className="centrado">
        <a href="/inicio">Volver a Mis cursos</a>
      </p>
    </main>
  )
}

export default Inscribirse
