import type { ReactNode } from 'react'
import { Ilustracion, type NombreIlustracion } from './Ilustracion.tsx'

// Piezas de maquetación que comparten varias pantallas. Los estilos están en diseno.css.

// Pantallas de acceso (iniciar sesión, registrarse, recuperar contraseña): el formulario a un lado y, en pantallas
// anchas, un panel de color con una ilustración y un lema al otro. El contenido va primero en el documento para el
// teclado y los lectores de pantalla; el panel solo se coloca a la izquierda con CSS.
export function Acceso({ lema, descripcion, children }: { lema: string; descripcion: string; children: ReactNode }) {
  return (
    <main className="acceso">
      <div className="acceso-contenido">{children}</div>
      <aside className="acceso-lateral ilu-oscuro">
        <Ilustracion nombre="acceso" />
        <h2>{lema}</h2>
        <p>{descripcion}</p>
      </aside>
    </main>
  )
}

// La franja grande de color con la que abre una pantalla: el texto a la izquierda y, si se da, algo a la derecha
// (el panel «Continuar», las cifras…). «clase» añade la paleta de un lenguaje (hero-curso leng-python).
export function Hero({ children, lateral, clase }: { children: ReactNode; lateral?: ReactNode; clase?: string }) {
  const clases = ['hero', lateral ? 'hero-con-lateral' : '', clase ?? ''].filter(Boolean).join(' ')
  return (
    <section className={clases}>
      <div className="hero-texto anima">{children}</div>
      {lateral}
    </section>
  )
}

// Para cuando no hay nada que mostrar («vacio») o algo salió bien («exito»): la ilustración y el mensaje de siempre.
export function EstadoVacio({ ilustracion = 'vacio', children }: { ilustracion?: NombreIlustracion; children: ReactNode }) {
  return (
    <div className={ilustracion === 'exito' ? 'estado-vacio estado-vacio-exito' : 'estado-vacio'}>
      <Ilustracion nombre={ilustracion} />
      {children}
    </div>
  )
}
