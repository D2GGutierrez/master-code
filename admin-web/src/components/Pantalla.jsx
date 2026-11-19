import { Logo } from './Ilustracion.jsx'
import Icono from './Icono.jsx'

// Piezas de maquetación que comparten varias pantallas. Los estilos están en diseno.css.

const BENEFICIOS = [
  { titulo: 'Problemas generados con IA', texto: 'Describe el tema y la IA propone problemas con su enunciado, casos de prueba y solución.' },
  { titulo: 'Tú decides qué se publica', texto: 'Revisa, edita y aprueba cada problema antes de que lo vean tus estudiantes.' },
  { titulo: 'Cursos con código de invitación', texto: 'Crea un curso, comparte el código y mira quién se une.' },
  { titulo: 'Sigue el avance del grupo', texto: 'Aciertos por problema y evaluaciones para saber qué reforzar en clase.' },
]

// Pantallas de acceso (iniciar sesión, registrarse, recuperar contraseña): el formulario a un lado y, en pantallas
// anchas, un panel oscuro con el lema, los beneficios y una ilustración al otro, como el registro de GitHub.
// El contenido va primero en el documento para el teclado y los lectores de pantalla; el panel solo se coloca a la
// izquierda con CSS. «enlace» es la invitación de arriba a la derecha («¿Ya tienes cuenta? Inicia sesión →»).
export function Acceso({ lema, descripcion, beneficios = BENEFICIOS, enlace, ilustracion = '/img/generacion-ia.webp', children }) {
  return (
    <main className="acceso">
      <div className="acceso-contenido">
        {enlace && (
          <p className="acceso-enlace-superior">
            {enlace.texto} <a href={enlace.href}>{enlace.accion} →</a>
          </p>
        )}
        <div className="acceso-formulario">{children}</div>
      </div>
      <aside className="acceso-lateral">
        <a className="marca acceso-marca" href="/">
          <Logo />
          MASTER CODE
        </a>
        <h2>{lema}</h2>
        <p>{descripcion}</p>
        <ul className="acceso-beneficios">
          {beneficios.map((b) => (
            <li key={b.titulo}>
              <Icono nombre="correcto" />
              <div>
                <strong>{b.titulo}</strong>
                <span>{b.texto}</span>
              </div>
            </li>
          ))}
        </ul>
        <img className="acceso-ilustracion" src={ilustracion} alt="" />
      </aside>
    </main>
  )
}

// Cada dibujo tiene una versión para el tema claro y otra para el oscuro; se elige según el tema del sistema.
const DIBUJOS = {
  docente: { ruta: '/img/np-docente', ancho: 640, alto: 489 },
  equipo: { ruta: '/img/np-equipo', ancho: 640, alto: 793 },
  celebracion: { ruta: '/img/np-celebracion', ancho: 640, alto: 484 },
  libros: { ruta: '/img/mc-libros', ancho: 640, alto: 506 },
  diseno: { ruta: '/img/mc-diseno', ancho: 640, alto: 481 },
  escalera: { ruta: '/img/mc-escalera', ancho: 640, alto: 480 },
  curso: { ruta: '/img/nc-curso', ancho: 960, alto: 745 },
  certificados: { ruta: '/img/mc-certificados', ancho: 640, alto: 502 },
}

// Un dibujo decorativo (alt vacío): lo que importa lo dice el texto de al lado.
export function Dibujo({ nombre, className }) {
  const d = DIBUJOS[nombre]
  return (
    <picture className={className}>
      <source srcSet={`${d.ruta}-oscuro.webp`} media="(prefers-color-scheme: dark)" />
      <img src={`${d.ruta}-claro.webp`} alt="" width={d.ancho} height={d.alto} />
    </picture>
  )
}

// La franja grande de color con la que abre una pantalla: el texto a la izquierda y, si se da, algo a la derecha.
// «clase» añade variantes (hero-compacto, hero-curso leng-python…); «ilustracion» es el nombre de un dibujo.
export function Hero({ children, lateral, clase, ilustracion }) {
  const clases = ['hero', lateral ? 'hero-con-lateral' : '', ilustracion ? 'hero-con-ilustracion' : '', clase ?? '']
    .filter(Boolean)
    .join(' ')
  return (
    <section className={clases}>
      <div className="hero-texto anima">{children}</div>
      {ilustracion && <Dibujo nombre={ilustracion} className="hero-ilustracion" />}
      {lateral}
    </section>
  )
}

const ICONO_DE = { vacio: 'libro', 'vacio-historial': 'codigo', exito: 'correcto' }

// Para cuando no hay nada que mostrar («vacio») o algo salió bien («exito»): la ilustración y el mensaje de siempre.
// «dibujo» pone un dibujo grande en lugar del icono.
export function EstadoVacio({ ilustracion = 'vacio', dibujo, children }) {
  const clases = ['estado-vacio', ilustracion === 'exito' ? 'estado-vacio-exito' : '', dibujo ? 'estado-vacio-con-dibujo' : '']
    .filter(Boolean)
    .join(' ')
  return (
    <div className={clases}>
      {dibujo ? (
        <Dibujo nombre={dibujo} className="estado-vacio-dibujo" />
      ) : (
        <Icono nombre={ICONO_DE[ilustracion] ?? 'libro'} className="estado-vacio-icono" />
      )}
      {children}
    </div>
  )
}
