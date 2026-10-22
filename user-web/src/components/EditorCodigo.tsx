import Editor from '@monaco-editor/react'

type Props = {
  valor: string
  onCambio: (valor: string) => void
  lenguaje?: string
  soloLectura?: boolean
  altura?: string
}

// Monaco se descarga de un CDN la primera vez que se muestra el editor (cargador por defecto del paquete).
function EditorCodigo({ valor, onCambio, lenguaje = 'plaintext', soloLectura = false, altura = '360px' }: Props) {
  return (
    <Editor
      value={valor}
      onChange={(texto) => onCambio(texto ?? '')}
      language={lenguaje}
      height={altura}
      theme="vs"
      loading="Cargando editor…"
      wrapperProps={{ 'aria-label': 'Editor de código' }}
      options={{
        lineNumbers: 'on',
        minimap: { enabled: false },
        fontSize: 14,
        tabSize: 4,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        readOnly: soloLectura,
      }}
    />
  )
}

export default EditorCodigo
