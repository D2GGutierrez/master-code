import { haySesion } from './api/sesion.js'
import BotonCerrarSesion from './components/BotonCerrarSesion.jsx'
import BandejaRevision from './pages/BandejaRevision.jsx'
import EditarCurso from './pages/EditarCurso.jsx'
import EstudiantesInscritos from './pages/EstudiantesInscritos.jsx'
import GenerarProblemas from './pages/GenerarProblemas.jsx'
import InicioSesion from './pages/InicioSesion.jsx'
import MisCursos from './pages/MisCursos.jsx'
import NuevaContrasena from './pages/NuevaContrasena.jsx'
import NuevoCurso from './pages/NuevoCurso.jsx'
import NuevoProblema from './pages/NuevoProblema.jsx'
import OlvideContrasena from './pages/OlvideContrasena.jsx'
import RegistroDocente from './pages/RegistroDocente.jsx'

// Sin enrutador todavía: la pantalla se elige por la ruta de la URL.
function pantalla(ruta) {
  if (ruta === '/olvide-contrasena') {
    return <OlvideContrasena />
  }
  // admin-api arma el enlace del correo con esta ruta.
  if (ruta === '/nueva-contrasena') {
    return <NuevaContrasena />
  }
  if (ruta === '/cursos') {
    return <MisCursos />
  }
  if (ruta === '/cursos/nuevo') {
    return <NuevoCurso />
  }
  // Anclada y solo con dígitos: un id no numérico cae en la pantalla por defecto.
  const edicion = ruta.match(/^\/cursos\/(\d+)\/editar$/)
  if (edicion) {
    return <EditarCurso id={Number(edicion[1])} />
  }
  const inscritos = ruta.match(/^\/cursos\/(\d+)\/inscritos$/)
  if (inscritos) {
    return <EstudiantesInscritos id={Number(inscritos[1])} />
  }
  const generacion = ruta.match(/^\/cursos\/(\d+)\/generar$/)
  if (generacion) {
    return <GenerarProblemas id={Number(generacion[1])} />
  }
  if (ruta === '/revision') {
    return <BandejaRevision />
  }
  if (ruta === '/problemas/nuevo') {
    return <NuevoProblema />
  }
  if (ruta === '/login') {
    return <InicioSesion />
  }
  return <RegistroDocente />
}

function App() {
  const ruta = window.location.pathname
  return (
    <>
      {haySesion() && ruta !== '/login' && (
        <header style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <BotonCerrarSesion />
        </header>
      )}
      {pantalla(ruta)}
    </>
  )
}

export default App
