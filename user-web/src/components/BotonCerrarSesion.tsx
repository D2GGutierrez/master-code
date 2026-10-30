import { useState } from 'react'
import { cerrarSesionEnServidor } from '../api/cuentas.ts'
import { borrarSesion } from '../api/sesion.ts'
import Icono from './Icono.tsx'

function BotonCerrarSesion() {
  const [cerrando, setCerrando] = useState(false)

  async function cerrar() {
    setCerrando(true)
    // Si el servidor falla o no responde se ignora: la sesión del navegador se cierra igual.
    await cerrarSesionEnServidor(localStorage.getItem('refresh') ?? '').catch(() => {})
    borrarSesion()
    window.location.assign('/login')
  }

  return (
    <button type="button" onClick={cerrar} disabled={cerrando}>
      <Icono nombre="salir" />
      {cerrando ? 'Cerrando sesión…' : 'Cerrar sesión'}
    </button>
  )
}

export default BotonCerrarSesion
