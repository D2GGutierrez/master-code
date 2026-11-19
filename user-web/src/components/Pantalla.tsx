import type { ReactNode } from 'react'
import { Logo, type NombreIlustracion } from './Ilustracion.tsx'
import Icono from './Icono.tsx'

// Piezas de maquetación que comparten varias pantallas. Los estilos están en diseno.css.

export type Beneficio = { titulo: string; texto: string }

const BENEFICIOS: Beneficio[] = [
  { titulo: 'Problemas hechos para tu curso', texto: 'Ejercicios que tu docente prepara con ayuda de la IA, según lo que están viendo en clase.' },
  { titulo: 'Practica con el Dojo', texto: 'Opción múltiple, completar el espacio y predecir la salida, sin necesidad de instalar nada.' },
  { titulo: 'Sube de cinturón', texto: 'Gana puntos en cada lenguaje, mantén tu racha y mide tu avance.' },
  { titulo: 'Reta a tus compañeros', texto: 'Duelos en tiempo real desde tu navegador o tu teléfono.' },
]

// Pantallas de acceso (iniciar sesión, registrarse, recuperar contraseña): el formulario a un lado y, en pantallas
// anchas, un panel oscuro con el lema, los beneficios y una ilustración al otro, como el registro de GitHub.
// El contenido va primero en el documento para el teclado y los lectores de pantalla; el panel solo se coloca a la
// izquierda con CSS. «enlace» es la invitación de arriba a la derecha («¿Ya tienes cuenta? Inicia sesión →»).
export function Acceso({
  lema,
  descripcion,
  beneficios = BENEFICIOS,
  enlace,
  ilustracion = '/img/mascota.webp',
  children,
}: {
  lema: string
  descripcion: string
  beneficios?: Beneficio[]
  enlace?: { texto: string; accion: string; href: string }
  ilustracion?: string
  children: ReactNode
}) {
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

export type NombreDibujo = 'intro' | 'escalera' | 'codigo' | 'cohete' | 'celebracion'

// Cada dibujo tiene una versión para el tema claro y otra para el oscuro. «borde» dice dónde el original viene
// recortado, para desvanecerlo en vez de cortarlo en seco.
const DIBUJOS: Record<NombreDibujo, { ruta: string; ancho: number; alto: number; borde: 'esquina' | 'todos' | 'ninguno' }> = {
  intro: { ruta: '/img/intro', ancho: 760, alto: 679, borde: 'esquina' },
  escalera: { ruta: '/img/av-escalera', ancho: 640, alto: 478, borde: 'ninguno' },
  codigo: { ruta: '/img/av-codigo', ancho: 640, alto: 478, borde: 'ninguno' },
  cohete: { ruta: '/img/av-cohete', ancho: 640, alto: 478, borde: 'ninguno' },
  celebracion: { ruta: '/img/av-celebracion', ancho: 640, alto: 571, borde: 'todos' },
}

// Un dibujo decorativo (alt vacío): lo que importa lo dice el texto de al lado.
export function Dibujo({ nombre, className }: { nombre: NombreDibujo; className?: string }) {
  const d = DIBUJOS[nombre]
  return (
    <picture className={className}>
      <source srcSet={`${d.ruta}-oscuro.webp`} media="(prefers-color-scheme: dark)" />
      <img className={`dibujo-borde-${d.borde}`} src={`${d.ruta}-claro.webp`} alt="" width={d.ancho} height={d.alto} />
    </picture>
  )
}

// La franja grande de color con la que abre una pantalla: el texto a la izquierda y, si se da, algo a la derecha
// (el panel «Continuar», las cifras…). «clase» añade la paleta de un lenguaje (hero-curso leng-python).
export function Hero({
  children,
  lateral,
  clase,
  ilustracion,
}: {
  children: ReactNode
  lateral?: ReactNode
  clase?: string
  // Un dibujo a la derecha del texto (la página de inicio). Hay una versión para el tema claro y otra para el oscuro.
  ilustracion?: boolean | NombreDibujo
}) {
  const clases = ['hero', lateral ? 'hero-con-lateral' : '', ilustracion ? 'hero-con-ilustracion' : '', clase ?? '']
    .filter(Boolean)
    .join(' ')
  return (
    <section className={clases}>
      <div className="hero-texto anima">{children}</div>
      {ilustracion && <Dibujo nombre={ilustracion === true ? 'intro' : ilustracion} className="hero-ilustracion" />}
      {lateral}
    </section>
  )
}

const ICONO_DE: Record<NombreIlustracion, 'libro' | 'codigo' | 'correcto'> = {
  vacio: 'libro',
  'vacio-historial': 'codigo',
  exito: 'correcto',
}

// Para cuando no hay nada que mostrar («vacio») o algo salió bien («exito»): la ilustración y el mensaje de siempre.
export function EstadoVacio({
  ilustracion = 'vacio',
  dibujo,
  children,
}: {
  ilustracion?: NombreIlustracion
  // Un dibujo grande en lugar del icono.
  dibujo?: NombreDibujo
  children: ReactNode
}) {
  return (
    <div className={[ 'estado-vacio', ilustracion === 'exito' ? 'estado-vacio-exito' : '', dibujo ? 'estado-vacio-con-dibujo' : '' ].filter(Boolean).join(' ')}>
      {dibujo ? (
        <Dibujo nombre={dibujo} className="estado-vacio-dibujo" />
      ) : (
        <Icono nombre={ICONO_DE[ilustracion]} className="estado-vacio-icono" />
      )}
      {children}
    </div>
  )
}
