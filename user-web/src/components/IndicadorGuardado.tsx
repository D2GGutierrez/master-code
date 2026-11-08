import { useState } from 'react'
import { textoGuardado } from '../guardado.ts'
import type { EstadoAutoguardado } from '../useAutoguardado.ts'
import './IndicadorGuardado.css'

type Props = {
  estado: EstadoAutoguardado
  ultimoGuardado: Date | null
}

// HU35-T4: el estado del borrador, discreto. La región existe siempre, aunque esté vacía, para que el lector de
// pantalla anuncie el primer cambio. Hora fija y sin temporizador: solo se anuncia cuando cambia el estado.
function IndicadorGuardado({ estado, ultimoGuardado }: Props) {
  // El día se compara con el de la apertura del editor: tras la medianoche, el guardado sale con su fecha.
  const [ahora] = useState(() => new Date())
  return (
    <p
      className={estado === 'error' ? 'indicador-guardado indicador-guardado-error' : 'indicador-guardado'}
      role="status"
      aria-live="polite"
    >
      {textoGuardado(estado, ultimoGuardado, ahora)}
    </p>
  )
}

export default IndicadorGuardado
