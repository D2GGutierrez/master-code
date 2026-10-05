import NuevaContrasena from './pages/NuevaContrasena.jsx'
import OlvideContrasena from './pages/OlvideContrasena.jsx'
import RegistroDocente from './pages/RegistroDocente.jsx'

// Sin enrutador todavía: la pantalla se elige por la ruta de la URL.
function App() {
  const ruta = window.location.pathname
  if (ruta === '/olvide-contrasena') {
    return <OlvideContrasena />
  }
  // admin-api arma el enlace del correo con esta ruta.
  if (ruta === '/nueva-contrasena') {
    return <NuevaContrasena />
  }
  return <RegistroDocente />
}

export default App
