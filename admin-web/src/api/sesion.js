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
