import { useEffect, useState } from 'react'
import { listarPendientes } from '../api/problemas.js'
import './EnlaceBandeja.css'

// HU18-T4: el enlace a la bandeja con cuántos pendientes hay. El número es secundario: mientras carga
// o si la petición falla, se muestra solo el enlace, sin errores ni reintentos.
function EnlaceBandeja() {
  const [pendientes, setPendientes] = useState(0)

  useEffect(() => {
    // Si se sale de la pantalla antes de la respuesta, no se actualiza un componente desmontado.
    let activo = true

    async function contar() {
      try {
        // admin-api devuelve solo los pendientes, en una lista simple sin total: se cuenta su largo.
        const { ok, cuerpo } = await listarPendientes()
        if (activo && ok && Array.isArray(cuerpo)) {
          setPendientes(cuerpo.length)
        }
      } catch {
        // Sin conexión: el enlace queda sin número.
      }
    }

    contar()
    return () => {
      activo = false
    }
  }, [])

  return (
    <a
      href="/revision"
      aria-label={
        pendientes > 0
          ? `Bandeja de revisión, ${pendientes} ${pendientes === 1 ? 'pendiente' : 'pendientes'}`
          : 'Bandeja de revisión'
      }
    >
      Bandeja de revisión
      {pendientes > 0 && (
        <>
          {' '}
          <span className="distintivo-pendientes">{pendientes > 99 ? '99+' : pendientes}</span>
        </>
      )}
    </a>
  )
}

export default EnlaceBandeja
