import { programarRenovacion } from './api/sesion.ts'
import Avance from './pages/Avance.tsx'
import InicioSesion from './pages/InicioSesion.tsx'
import NuevaContrasena from './pages/NuevaContrasena.tsx'
import OlvideContrasena from './pages/OlvideContrasena.tsx'
import RegistroEstudiante from './pages/RegistroEstudiante.tsx'

// Se ejecuta una vez al cargar: si quedó una sesión guardada (p. ej. al recargar), se reanuda su renovación.
programarRenovacion()

// Sin enrutador todavía: la pantalla se elige por la ruta de la URL.
function App() {
  if (window.location.pathname === '/login') {
    return <InicioSesion />
  }
  if (window.location.pathname === '/avance') {
    return <Avance />
  }
  if (window.location.pathname === '/olvide-contrasena') {
    return <OlvideContrasena />
  }
  if (window.location.pathname === '/nueva-contrasena') {
    return <NuevaContrasena />
  }
  return <RegistroEstudiante />
}

export default App
