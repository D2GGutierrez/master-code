import { correoDeSesion, haySesion } from '../api/sesion.js'
import BotonCerrarSesion from './BotonCerrarSesion.jsx'
import { Logo } from './Ilustracion.jsx'

const ENLACES = [
  { ruta: '/cursos', texto: 'Mis cursos' },
  { ruta: '/cursos/nuevo', texto: 'Nuevo curso' },
  { ruta: '/problemas/nuevo', texto: 'Nuevo problema' },
]

// Barra superior oscura: la marca siempre; con la sesión iniciada, los lugares a los que se vuelve a menudo y
// «Cerrar sesión». En /login no se ofrece cerrar sesión: ahí se llega justamente sin una.
function Cabecera() {
  const ruta = window.location.pathname
  const conSesion = haySesion() && ruta !== '/login'
  const correo = conSesion ? correoDeSesion() : null
  return (
    <header className="cabecera">
      <div className="cabecera-interior">
        <a className="marca" href={conSesion ? '/cursos' : '/login'}>
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
              {correo ? correo.charAt(0).toUpperCase() : '?'}
            </span>
            <BotonCerrarSesion />
          </div>
        )}
      </div>
    </header>
  )
}

export default Cabecera
