import type { ProblemaDetalle } from '../api/problemas.ts'
import { etiquetaDificultad, etiquetaTipoEjercicio } from '../etiquetas.ts'
import Enunciado from './Enunciado.tsx'

type Props = {
  problema: Pick<ProblemaDetalle, 'titulo' | 'dificultad' | 'tipo_ejercicio' | 'enunciado'>
}

// Panel izquierdo del editor (HU33-T1). Los ejemplos son HU33-T3.
function PanelEnunciado({ problema }: Props) {
  return (
    <section className="panel-enunciado" aria-label="Enunciado">
      <h2>{problema.titulo}</h2>
      <p className="problema-etiquetas">
        <span className="etiqueta-problema">{etiquetaDificultad(problema.dificultad)}</span>
        <span className="etiqueta-problema">{etiquetaTipoEjercicio(problema.tipo_ejercicio)}</span>
      </p>
      <Enunciado texto={problema.enunciado} />
    </section>
  )
}

export default PanelEnunciado
