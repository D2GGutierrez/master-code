// Las ilustraciones son imágenes WebP con fondo transparente (public/img). Son decorativas: el texto de al lado ya dice
// lo mismo, por eso van con alt vacío y ocultas para los lectores de pantalla. Llevan ancho y alto para que la página
// no salte mientras cargan.

const IMAGENES = {
  'vacio': { archivo: 'vacio-cursos', ancho: 406, alto: 520 },
  'vacio-historial': { archivo: 'vacio-historial', ancho: 520, alto: 402 },
  'exito': { archivo: 'exito', ancho: 520, alto: 506 },
}

export function Ilustracion({ nombre, className }) {
  const { archivo, ancho, alto } = IMAGENES[nombre]
  return (
    <img
      className={className ? `ilustracion ${className}` : 'ilustracion'}
      src={`/img/${archivo}.webp`}
      width={ancho}
      height={alto}
      alt=""
      aria-hidden="true"
      loading="lazy"
      decoding="async"
    />
  )
}

// La marca: el escudo con los corchetes del código, en su versión clara para la barra oscura.
export function Logo() {
  return <img className="logo" src="/img/logo-simbolo-claro.webp" width="256" height="256" alt="" aria-hidden="true" />
}
