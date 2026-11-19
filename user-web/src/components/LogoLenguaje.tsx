import { claveLenguaje } from '../lenguajes.ts'

// El logo oficial del lenguaje junto a su nombre. Un lenguaje sin logo (los que no son python, java ni javascript)
// no dibuja nada y el nombre se queda solo. Es decorativo: el nombre al lado ya lo dice.
export function LogoLenguaje({ lenguaje }: { lenguaje: string }) {
  const clave = claveLenguaje(lenguaje)
  if (clave === 'otro') {
    return null
  }
  return <img className="logo-leng" src={`/img/leng-${clave}.webp`} alt="" width={16} height={16} />
}
