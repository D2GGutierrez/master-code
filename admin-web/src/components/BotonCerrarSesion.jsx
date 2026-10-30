import { useState } from 'react'
import { cerrarSesionEnServidor } from '../api/cuentas.js'
import { borrarRutaGuardada, borrarTokens, leerRefresh } from '../api/sesion.js'
import Icono from './Icono.jsx'

function BotonCerrarSesion() {
  const [cerrando, setCerrando] = useState(false)

  async function cerrar() {
    setCerrando(true)
    // Si el servidor falla o no responde se ignora: la sesión del navegador se cierra igual.
    await cerrarSesionEnServidor(leerRefresh()).catch(() => {})
    borrarTokens()
    // Sin ruta recordada, /login no muestra «Tu sesión expiró» ni devuelve a una pantalla anterior.
    borrarRutaGuardada()
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
