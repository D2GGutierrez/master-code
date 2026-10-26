import DOMPurify from 'dompurify'

// El enunciado se guarda como HTML sin sanitizar (editor Tiptap del docente o la IA): aquí se limpia antes de
// mostrarlo. Solo formato de texto; nada de estilos, imágenes, iframes, SVG ni formularios.
const ETIQUETAS_PERMITIDAS = [
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'code', 'pre', 'ul', 'ol', 'li',
  'h1', 'h2', 'h3', 'h4', 'blockquote', 'a', 'hr',
]
// Solo enlaces http, https y mailto: fuera javascript:, data: y cualquier otro esquema.
const URI_PERMITIDA = /^(?:https?:|mailto:)/i

// Los enlaces se abren en otra pestaña sin darle a esa página acceso a esta (window.opener).
DOMPurify.addHook('afterSanitizeAttributes', (nodo) => {
  if (nodo.tagName === 'A') {
    nodo.setAttribute('target', '_blank')
    nodo.setAttribute('rel', 'noopener noreferrer')
  }
})

// Sin etiquetas (p. ej. lo que genera la IA) se muestra como texto plano, con sus saltos de línea.
export function limpiarEnunciado(texto: string): { html: string } | { texto: string } {
  if (!texto.trim()) {
    return { texto: '' }
  }
  if (!/<[a-z][\s\S]*>/i.test(texto)) {
    return { texto }
  }
  return {
    html: DOMPurify.sanitize(texto, {
      ALLOWED_TAGS: ETIQUETAS_PERMITIDAS,
      ALLOWED_ATTR: ['href'],
      ALLOWED_URI_REGEXP: URI_PERMITIDA,
    }),
  }
}
