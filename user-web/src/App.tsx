import { programarRenovacion } from './api/sesion.ts'
import Avance from './pages/Avance.tsx'
import EditorPrueba from './pages/EditorPrueba.tsx'
import Inicio from './pages/Inicio.tsx'
import InicioSesion from './pages/InicioSesion.tsx'
import NuevaContrasena from './pages/NuevaContrasena.tsx'
import OlvideContrasena from './pages/OlvideContrasena.tsx'
import ProblemasCurso from './pages/ProblemasCurso.tsx'
import RegistroEstudiante from './pages/RegistroEstudiante.tsx'

// Se ejecuta una vez al cargar: si quedó una sesión guardada (p. ej. al recargar), se reanuda su renovación.
programarRenovacion()

// Sin enrutador todavía: la pantalla se elige por la ruta de la URL.
function App() {
  if (window.location.pathname === '/login') {
    return <InicioSesion />
  }
  if (window.location.pathname === '/inicio') {
    return <Inicio />
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
  if (window.location.pathname === '/editor-prueba') {
    return <EditorPrueba />
  }
  // Anclada y solo con dígitos: un id no numérico cae en la pantalla por defecto.
  const problemasCurso = window.location.pathname.match(/^\/cursos\/(\d+)\/problemas$/)
  if (problemasCurso) {
    return <ProblemasCurso id={Number(problemasCurso[1])} />
  }
  return <RegistroEstudiante />
}

export default App
