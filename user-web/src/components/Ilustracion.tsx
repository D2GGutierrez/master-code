// Ilustraciones propias en SVG, sin imágenes ni dependencias. Usan las clases ilu-* de diseno.css (que leen las
// mismas variables de color que el resto de la página), así que cambian solas con el tema claro u oscuro.
// Son decorativas: el texto de al lado ya dice lo mismo, por eso van ocultas para los lectores de pantalla.

import { estrella } from '../dibujo.ts'

const DIBUJOS = {
  // Una ventana de código con un resultado correcto y una barra de progreso flotando a su lado.
  acceso: (
    <>
      <circle className="ilu-halo" cx="240" cy="190" r="165" />
      <rect className="ilu-panel" x="82" y="82" width="316" height="216" rx="16" />
      <path className="ilu-linea" d="M82 122h316" />
      <circle className="ilu-acento" cx="104" cy="102" r="5" />
      <circle className="ilu-barra" cx="122" cy="102" r="5" />
      <circle className="ilu-barra" cx="140" cy="102" r="5" />
      {[148, 170, 192, 214, 236, 258].map((y) => (
        <rect key={y} className="ilu-barra" x="106" y={y} width="10" height="6" rx="3" />
      ))}
      <rect className="ilu-acento" x="134" y="146" width="56" height="10" rx="5" />
      <rect className="ilu-barra" x="198" y="146" width="96" height="10" rx="5" />
      <rect className="ilu-barra" x="134" y="168" width="44" height="10" rx="5" />
      <rect className="ilu-acento" x="186" y="168" width="74" height="10" rx="5" />
      <rect className="ilu-barra" x="154" y="190" width="112" height="10" rx="5" />
      <rect className="ilu-acento" x="154" y="212" width="68" height="10" rx="5" />
      <rect className="ilu-barra" x="230" y="212" width="52" height="10" rx="5" />
      <rect className="ilu-barra" x="134" y="234" width="36" height="10" rx="5" />
      <rect className="ilu-acento" x="178" y="234" width="40" height="10" rx="5" />
      <rect className="ilu-barra" x="134" y="256" width="126" height="10" rx="5" />
      <rect className="ilu-acento" x="268" y="253" width="3" height="16" rx="1.5" />

      <rect className="ilu-panel" x="336" y="48" width="116" height="60" rx="14" />
      <circle className="ilu-acento" cx="364" cy="78" r="14" />
      <path className="ilu-trazo-claro" d="M357 78l5 5 9-10" />
      <rect className="ilu-barra" x="386" y="68" width="52" height="8" rx="4" />
      <rect className="ilu-barra" x="386" y="84" width="34" height="8" rx="4" />

      <rect className="ilu-panel" x="28" y="270" width="140" height="68" rx="14" />
      <rect className="ilu-barra" x="46" y="287" width="60" height="8" rx="4" />
      <rect className="ilu-barra" x="46" y="308" width="104" height="10" rx="5" />
      <rect className="ilu-acento" x="46" y="308" width="68" height="10" rx="5" />

      <path className="ilu-trazo-acento" d="M62 140v16M54 148h16" />
      <circle className="ilu-acento-suave" cx="438" cy="262" r="8" />
      <circle className="ilu-brillo" cx="412" cy="330" r="4" />
      <path className="ilu-brillo" d={estrella(44, 70, 9)} />
    </>
  ),

  // Una caja vacía con algo por llegar.
  vacio: (
    <>
      <ellipse className="ilu-fondo" cx="120" cy="104" rx="98" ry="64" />
      <path className="ilu-panel-suave" d="M70 84l14-26h72l14 26z" />
      <path className="ilu-panel" d="M62 84h116v52a12 12 0 0 1-12 12H74a12 12 0 0 1-12-12z" />
      <rect className="ilu-barra" x="86" y="106" width="68" height="8" rx="4" />
      <rect className="ilu-barra" x="86" y="122" width="44" height="8" rx="4" />
      <circle className="ilu-trazo-acento" style={{ strokeDasharray: '3 6' }} cx="178" cy="46" r="18" />
      <path className="ilu-trazo-acento" d="M178 38v16M170 46h16" />
      <path className="ilu-brillo" d={estrella(54, 50, 8)} />
      <circle className="ilu-brillo" cx="196" cy="116" r="3.5" />
    </>
  ),

  // Una medalla: el resultado salió bien.
  exito: (
    <>
      <circle className="ilu-fondo" cx="100" cy="84" r="66" />
      <path className="ilu-acento-suave" d="M78 112l-14 36 20-8 10 14 8-38z" />
      <path className="ilu-acento-suave" d="M122 112l14 36-20-8-10 14-8-38z" />
      <circle className="ilu-panel" cx="100" cy="80" r="38" />
      <circle className="ilu-acento" cx="100" cy="80" r="27" />
      <path className="ilu-trazo-claro" style={{ strokeWidth: 4 }} d="M88 81l8 8 17-18" />
      <path className="ilu-brillo" d={estrella(34, 42, 8)} />
      <path className="ilu-brillo" d={estrella(168, 50, 6)} />
      <path className="ilu-brillo" d={estrella(164, 122, 7)} />
      <circle className="ilu-brillo" cx="26" cy="110" r="3.5" />
    </>
  ),
}

const MEDIDAS = {
  acceso: '0 0 480 380',
  vacio: '0 0 240 180',
  exito: '0 0 200 160',
}

export type NombreIlustracion = keyof typeof DIBUJOS

export function Ilustracion({ nombre, className }: { nombre: NombreIlustracion; className?: string }) {
  return (
    <svg
      className={className ? `ilustracion ${className}` : 'ilustracion'}
      viewBox={MEDIDAS[nombre]}
      aria-hidden="true"
      focusable="false"
    >
      {DIBUJOS[nombre]}
    </svg>
  )
}

// La marca: un cuadrado con degradado y los corchetes del código. El degradado se define aquí mismo; la marca
// aparece una sola vez por pantalla, así que el id no se repite.
export function Logo() {
  return (
    <svg className="logo" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id="marca-degradado" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#8b5cf6" />
          <stop offset="1" stopColor="#38bdf8" />
        </linearGradient>
      </defs>
      <rect width="24" height="24" rx="7" fill="url(#marca-degradado)" />
      <path fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M10 8l-4 4 4 4M14 8l4 4-4 4" />
    </svg>
  )
}
