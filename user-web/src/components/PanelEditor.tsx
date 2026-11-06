import BotonCodigoBase from './BotonCodigoBase.tsx'
import EjecutorCodigo from './EjecutorCodigo.tsx'
import EnvioSolucion from './EnvioSolucion.tsx'

type Props = {
  problemaId: number
  lenguaje: string
  codigo: string
  onCambioCodigo: (codigo: string) => void
  codigoBase: string | null | undefined
  alRestaurarCodigoBase: () => void
}

// Panel derecho del editor (HU33-T1). El código vive en EditorProblema y llega por props.
function PanelEditor({ problemaId, lenguaje, codigo, onCambioCodigo, codigoBase, alRestaurarCodigoBase }: Props) {
  return (
    <>
      <BotonCodigoBase codigoBase={codigoBase} codigoActual={codigo} alRestaurar={alRestaurarCodigoBase} />
      {/* Igual que /editor-prueba: EjecutorCodigo ya incluye el editor y traduce el lenguaje con idMonaco. */}
      <EjecutorCodigo valor={codigo} onCambio={onCambioCodigo} lenguaje={lenguaje} />
      <EnvioSolucion problemaId={problemaId} codigo={codigo} />
    </>
  )
}

export default PanelEditor
