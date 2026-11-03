import './Ejemplos.css'

// Lo que llega en casos_prueba del detalle (user-api solo manda los públicos). es_publico es el nombre de la marca en
// admin-api: hoy no viaja, pero si llegara en falso el caso no se muestra (red de seguridad, HU31).
type Caso = {
  entrada: string
  salida_esperada: string
  es_publico?: boolean
}

type Props = {
  casos: Caso[]
  // Nivel del encabezado «Ejemplos»; cada «Ejemplo N» va un nivel más abajo.
  nivel: 2 | 3
  clase?: string
}

function Bloque({ texto, rotulo, vacio }: { texto: string; rotulo: string; vacio: string }) {
  // Solo una cadena vacía es «vacía»: espacios o saltos de línea son parte de lo que lee o imprime el programa.
  return texto ? (
    // Enfocable para desplazarlo con el teclado cuando no cabe.
    <pre tabIndex={0} aria-label={rotulo}>
      {texto}
    </pre>
  ) : (
    <p className="ejemplo-vacio">{vacio}</p>
  )
}

// HU33-T3: los ejemplos de entrada y salida, iguales en el detalle del problema y en el panel del editor.
function Ejemplos({ casos, nivel, clase }: Props) {
  const publicos = casos.filter((caso) => caso.es_publico !== false)
  if (publicos.length === 0) {
    return null
  }
  const Titulo = nivel === 2 ? 'h2' : 'h3'
  const Subtitulo = nivel === 2 ? 'h3' : 'h4'
  return (
    <section className={clase ? `ejemplos ${clase}` : 'ejemplos'}>
      <Titulo>Ejemplos</Titulo>
      {publicos.map((caso, indice) => (
        // Los casos no traen id; el orden lo fija el servidor y no cambia.
        <div key={indice} className="ejemplo">
          <Subtitulo>Ejemplo {indice + 1}</Subtitulo>
          <p className="ejemplo-rotulo">Entrada</p>
          <Bloque texto={caso.entrada} rotulo={`Entrada del ejemplo ${indice + 1}`} vacio="Sin entrada" />
          <p className="ejemplo-rotulo">Salida esperada</p>
          <Bloque
            texto={caso.salida_esperada}
            rotulo={`Salida esperada del ejemplo ${indice + 1}`}
            vacio="Salida vacía"
          />
        </div>
      ))}
    </section>
  )
}

export default Ejemplos
