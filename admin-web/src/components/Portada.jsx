import { claveLenguaje } from '../lenguajes.js'

// La «imagen» de un curso: una ilustración con los colores de su lenguaje (public/img/portada-*.webp). Es decorativa:
// el nombre y el lenguaje ya están escritos al lado. Hay tres variantes por lenguaje y se elige con el número del
// curso (la misma imagen normal, reflejada o acercada), así dos cursos del mismo lenguaje no se ven idénticos.
// El degradado de .portada queda de fondo mientras la imagen carga.
export function Portada({ lenguaje, variante = 0, className }) {
  const clave = claveLenguaje(lenguaje)
  const indice = Math.abs(Math.trunc(variante)) % 3
  return (
    <div className={`portada leng-${clave} portada-v${indice}${className ? ` ${className}` : ''}`} aria-hidden="true">
      <img src={`/img/portada-${clave}.webp`} width="960" height="540" alt="" loading="lazy" decoding="async" />
    </div>
  )
}
