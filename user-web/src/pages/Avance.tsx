import { useEffect, useState } from 'react'
import { obtenerAvance, type Avance as DatosAvance } from '../api/avance.ts'

function Avance() {
  const [avance, setAvance] = useState<DatosAvance | null>(null)
  const [error, setError] = useState(false)

  function cargar() {
    obtenerAvance().then(setAvance, () => setError(true))
  }

  function reintentar() {
    setError(false)
    cargar()
  }

  useEffect(() => {
    if (!localStorage.getItem('access')) {
      window.location.assign('/login')
      return
    }
    cargar()
  }, [])

  return (
    <main>
      <h1>Mi avance</h1>
      {error ? (
        <>
          <p role="alert">No se pudo cargar tu avance. Inténtalo de nuevo.</p>
          <button type="button" onClick={reintentar}>Reintentar</button>
        </>
      ) : !avance ? (
        <p>Cargando tu avance…</p>
      ) : avance.total_envios === 0 ? (
        <p>Aún no has resuelto ejercicios. Cuando lo hagas, desde la web o desde el móvil, tu avance aparecerá aquí.</p>
      ) : (
        <>
          {/* user-api cuenta intentos, no resueltos: app_envio aún no guarda el resultado. */}
          <p>Problemas intentados: {avance.problemas_intentados}</p>
          <p>
            Envíos: {avance.total_envios} (web: {avance.desde_web}, móvil: {avance.desde_movil})
          </p>
          <table>
            <thead>
              <tr>
                <th>Problema</th>
                <th>Envíos</th>
                <th>Web</th>
                <th>Móvil</th>
                <th>Último envío</th>
              </tr>
            </thead>
            <tbody>
              {avance.problemas.map((problema) => (
                <tr key={problema.problema_id}>
                  <td>{problema.problema_id}</td>
                  <td>{problema.envios}</td>
                  <td>{problema.desde_web}</td>
                  <td>{problema.desde_movil}</td>
                  <td>{new Date(problema.ultimo_envio).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </main>
  )
}

export default Avance
