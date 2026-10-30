import { correoDeSesion } from '../api/sesion.ts'
import { inicialDeCorreo } from '../persona.ts'
import BotonCerrarSesion from './BotonCerrarSesion.tsx'
import { Logo } from './Ilustracion.tsx'

const ENLACES = [
  { ruta: '/inicio', texto: 'Mis cursos' },
  { ruta: '/cursos/buscar', texto: 'Buscar cursos' },
  { ruta: '/avance', texto: 'Mi avance' },
]

// Barra superior oscura: la marca y, con la sesión iniciada, los tres lugares a los que se vuelve siempre, la
// inicial de quien está dentro y «Cerrar sesión».
function Cabecera() {
  const conSesion = Boolean(localStorage.getItem('access'))
  const ruta = window.location.pathname
  const correo = conSesion ? correoDeSesion() : null
  return (
    <header className="cabecera">
      <div className="cabecera-interior">
        <a className="marca" href={conSesion ? '/inicio' : '/login'}>
          <Logo />
          Master Code
        </a>
        {conSesion && (
          <nav aria-label="Principal">
            {ENLACES.map((enlace) => (
              <a key={enlace.ruta} href={enlace.ruta} aria-current={ruta === enlace.ruta ? 'page' : undefined}>
                {enlace.texto}
              </a>
            ))}
          </nav>
        )}
        {conSesion && (
          <div className="cabecera-fin">
            <span className="avatar" aria-hidden="true" title={correo ?? undefined}>
              {inicialDeCorreo(correo)}
            </span>
            <BotonCerrarSesion />
          </div>
        )}
      </div>
    </header>
  )
}

export default Cabecera
