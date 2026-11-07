import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { guardarBorrador } from '../api/borradores.ts'
import './BotonCopiarAlEditor.css'

type Props = {
  problemaId: number
  codigo: string
  numeroIntento?: number
  navegar?: (ruta: string) => void
}

function irA(ruta: string) {
  window.location.assign(ruta)
}

// HU42-T4 (D8): deja el código de un envío como borrador del problema, tras confirmar, y abre el editor, que arranca
// con el borrador. La confirmación es en línea, como la de «Volver al código base original».
function BotonCopiarAlEditor({ problemaId, codigo, numeroIntento, navegar = irA }: Props) {
  const [confirmando, setConfirmando] = useState(false)
  const [copiando, setCopiando] = useState(false)
  const [error, setError] = useState(false)
  const boton = useRef<HTMLButtonElement>(null)
  const cancelar = useRef<HTMLButtonElement>(null)
  const copiar = useRef<HTMLButtonElement>(null)
  // Un segundo clic en el mismo instante llega antes de que React deshabilite el botón: esto lo frena.
  const enCurso = useRef(false)
  // Para no mover el foco al montar: solo al cerrar una confirmación que se abrió.
  const seAbrio = useRef(false)
  const id = useId()

  useEffect(() => {
    if (confirmando) {
      cancelar.current?.focus()
    } else if (seAbrio.current) {
      boton.current?.focus()
    }
  }, [confirmando])

  // Al fallar, el botón que tenía el foco estuvo deshabilitado y lo perdió: vuelve a él para reintentar.
  useEffect(() => {
    if (error) {
      copiar.current?.focus()
    }
  }, [error])

  if (!codigo.trim()) {
    return null
  }

  function abrir() {
    seAbrio.current = true
    setError(false)
    setConfirmando(true)
  }

  async function confirmar() {
    if (enCurso.current) {
      return
    }
    enCurso.current = true
    setError(false)
    setCopiando(true)
    try {
      await guardarBorrador(problemaId, codigo)
      setConfirmando(false)
      navegar(`/problemas/${problemaId}/editor`)
    } catch {
      setError(true)
    } finally {
      enCurso.current = false
      setCopiando(false)
    }
  }

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === 'Escape' && !copiando) {
      setConfirmando(false)
    }
  }

  return (
    <div className="copiar-editor">
      {confirmando ? (
        <div
          className="copiar-editor-confirmacion"
          role="group"
          aria-labelledby={`${id}-titulo`}
          aria-describedby={`${id}-texto`}
          onKeyDown={alTeclear}
        >
          <p id={`${id}-titulo`} className="copiar-editor-titulo">
            ¿Copiar este código al editor?
          </p>
          <p id={`${id}-texto`}>
            El código de este envío reemplazará el código que hay ahora en tu editor y tu borrador guardado de este
            problema. Esta acción no se puede deshacer.
          </p>
          {error && <p role="alert">No se pudo copiar el código al editor. Inténtalo de nuevo.</p>}
          {/* «Cancelar» recibe el foco: un Enter por accidente no reemplaza el borrador. */}
          <button ref={cancelar} type="button" onClick={() => setConfirmando(false)} disabled={copiando}>
            Cancelar
          </button>{' '}
          <button
            ref={copiar}
            type="button"
            className="boton-primario"
            onClick={confirmar}
            disabled={copiando}
            aria-busy={copiando}
          >
            {copiando ? 'Copiando…' : 'Copiar al editor'}
          </button>
        </div>
      ) : (
        <button
          ref={boton}
          type="button"
          onClick={abrir}
          aria-label={numeroIntento ? `Copiar al editor el código del intento ${numeroIntento}` : undefined}
        >
          Copiar al editor actual
        </button>
      )}
    </div>
  )
}

export default BotonCopiarAlEditor
