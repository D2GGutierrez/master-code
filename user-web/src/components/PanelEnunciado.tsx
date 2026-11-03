import type { ProblemaDetalle } from '../api/problemas.ts'
import { etiquetaDificultad, etiquetaTipoEjercicio } from '../etiquetas.ts'
import Ejemplos from './Ejemplos.tsx'
import Enunciado from './Enunciado.tsx'
import './PanelEnunciado.css'

type Props = {
  problema: Pick<ProblemaDetalle, 'titulo' | 'dificultad' | 'tipo_ejercicio' | 'enunciado' | 'casos_prueba'>
}

// Panel izquierdo del editor (HU33-T1 a T3). La página ya tiene su h1 (la fila de arriba): el título del problema es
// h2, y «Enunciado» y «Ejemplos», h3.
function PanelEnunciado({ problema }: Props) {
  return (
    <section className="panel-enunciado" aria-label="Enunciado">
      <h2>{problema.titulo}</h2>
      <p className="problema-etiquetas">
        <span className="etiqueta-problema">{etiquetaDificultad(problema.dificultad)}</span>
        <span className="etiqueta-problema">{etiquetaTipoEjercicio(problema.tipo_ejercicio)}</span>
      </p>
      <h3 className="panel-enunciado-rotulo">Enunciado</h3>
      {problema.enunciado.trim() ? (
        <Enunciado texto={problema.enunciado} />
      ) : (
        <p className="panel-enunciado-vacio">Este problema no tiene enunciado.</p>
      )}
      <Ejemplos casos={problema.casos_prueba} nivel={3} />
    </section>
  )
}

export default PanelEnunciado
