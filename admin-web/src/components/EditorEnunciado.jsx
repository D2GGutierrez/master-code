import { EditorContent, useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import './EditorEnunciado.css'

// Los seis formatos de la barra: texto visible, nombre en Tiptap (para saber si está activo) y la orden.
const BOTONES = [
  { texto: 'Negrita', nombre: 'bold', alternar: (orden) => orden.toggleBold() },
  { texto: 'Cursiva', nombre: 'italic', alternar: (orden) => orden.toggleItalic() },
  { texto: 'Título', nombre: 'heading', atributos: { level: 2 }, alternar: (orden) => orden.toggleHeading({ level: 2 }) },
  { texto: 'Lista con viñetas', nombre: 'bulletList', alternar: (orden) => orden.toggleBulletList() },
  { texto: 'Lista numerada', nombre: 'orderedList', alternar: (orden) => orden.toggleOrderedList() },
  { texto: 'Bloque de código', nombre: 'codeBlock', alternar: (orden) => orden.toggleCodeBlock() },
]

function EditorEnunciado({ id, etiqueta, valor, onCambio }) {
  const editor = useEditor({
    // Sin enlaces: no son parte de esta tarea y StarterKit los crearía solos al pegar una URL.
    extensions: [StarterKit.configure({ link: false })],
    content: valor,
    // Vuelve a dibujar en cada cambio para que aria-pressed refleje el formato activo.
    shouldRerenderOnTransaction: true,
    editorProps: {
      // Un div editable no se enlaza con <label for>: role y aria-label le dan nombre accesible.
      attributes: { id, role: 'textbox', 'aria-multiline': 'true', 'aria-label': etiqueta, class: 'editor-contenido' },
    },
    // Sin texto, Tiptap deja <p></p>: se entrega "" para que el servidor lo trate como vacío.
    onUpdate: ({ editor: actual }) => onCambio(actual.getText().trim() ? actual.getHTML() : ''),
  })

  return (
    <div>
      <div className="editor-barra">
        {BOTONES.map((boton) => (
          <button
            key={boton.texto}
            // type="button": un botón dentro de un <form> lo enviaría.
            type="button"
            aria-pressed={editor.isActive(boton.nombre, boton.atributos)}
            onClick={() => boton.alternar(editor.chain().focus()).run()}
          >
            {boton.texto}
          </button>
        ))}
      </div>
      <EditorContent editor={editor} />
    </div>
  )
}

export default EditorEnunciado
