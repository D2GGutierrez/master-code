import { useState } from 'react'
import EditorCodigo from '../components/EditorCodigo.tsx'

// Página provisional para probar el editor; la reemplaza el layout de la pantalla del problema (HU33).
function EditorPrueba() {
  const [codigo, setCodigo] = useState('')

  return (
    <main>
      <h1>Editor de prueba</h1>
      <EditorCodigo valor={codigo} onCambio={setCodigo} />
      <pre>
        Caracteres: {codigo.length} · Líneas: {codigo.split('\n').length}
      </pre>
    </main>
  )
}

export default EditorPrueba
