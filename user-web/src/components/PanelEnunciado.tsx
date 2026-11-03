import type { ProblemaDetalle } from '../api/problemas.ts'
import { etiquetaDificultad, etiquetaTipoEjercicio } from '../etiquetas.ts'
import Enunciado from './Enunciado.tsx'
import './PanelEnunciado.css'

type Props = {
  problema: Pick<ProblemaDetalle, 'titulo' | 'dificultad' | 'tipo_ejercicio' | 'enunciado'>
}

// Panel izquierdo del editor (HU33-T1, HU33-T2). La página ya tiene su h1 (la fila de arriba): el título del problema es
// h2 y «Enunciado», h3. Los ejemplos son HU33-T3.
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
    </section>
  )
}

export default PanelEnunciado
