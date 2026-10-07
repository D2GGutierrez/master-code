// localStorage para que la sesión sobreviva a recargar la página (decisión simple del MVP).
export function guardarTokens(access, refresh) {
  localStorage.setItem('access', access)
  localStorage.setItem('refresh', refresh)
}

export function leerAccess() {
  return localStorage.getItem('access')
}

export function leerRefresh() {
  return localStorage.getItem('refresh')
}

export function borrarTokens() {
  localStorage.removeItem('access')
  localStorage.removeItem('refresh')
}

// sessionStorage: la ruta se recuerda solo en esta pestaña.
const CLAVE_RUTA = 'rutaAntesDelLogin'

// La sesión expiró del todo: se borra, se recuerda dónde estaba el docente y se le lleva al login.
export function irAlLogin() {
  borrarTokens()
  const ruta = window.location.pathname
  // En /login no se guarda ni se redirige: sería guardar /login como destino y entrar en bucle.
  if (ruta === '/login') {
    return
  }
  // Si varias peticiones fallan a la vez, se conserva la primera ruta guardada.
  if (!sessionStorage.getItem(CLAVE_RUTA)) {
    sessionStorage.setItem(CLAVE_RUTA, ruta + window.location.search)
  }
  window.location.assign('/login')
}

export function hayRutaGuardada() {
  return sessionStorage.getItem(CLAVE_RUTA) !== null
}

// Devuelve la ruta a la que volver tras iniciar sesión y la borra, para usarla una sola vez.
export function tomarRutaGuardada() {
  const ruta = sessionStorage.getItem(CLAVE_RUTA)
  sessionStorage.removeItem(CLAVE_RUTA)
  // Se valida porque sessionStorage se puede manipular: solo rutas de esta misma web.
  // //, \ y :// harían que el navegador saliera a otro sitio (redirección abierta).
  const esRutaInterna =
    ruta !== null &&
    ruta.startsWith('/') &&
    !ruta.startsWith('//') &&
    !ruta.includes('\\') &&
    !ruta.includes('://') &&
    !ruta.startsWith('/login')
  return esRutaInterna ? ruta : '/cursos'
}
