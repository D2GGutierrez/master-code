import { useState } from 'react'
import EditorCodigo from '../components/EditorCodigo.tsx'

const LENGUAJES = [
  {
    valor: 'python',
    etiqueta: 'Python',
    ejemplo: ['def saludar(nombre):', '    return f"Hola, {nombre}"', '', 'print(saludar("Ana"))'].join('\n'),
  },
  {
    valor: 'java',
    etiqueta: 'Java',
    ejemplo: [
      'public class Main {',
      '    public static void main(String[] args) {',
      '        System.out.println("Hola, Ana");',
      '    }',
      '}',
    ].join('\n'),
  },
  {
    valor: 'javascript',
    etiqueta: 'JavaScript',
    ejemplo: [
      'function saludar(nombre) {',
      '    return `Hola, ${nombre}`;',
      '}',
      '',
      "console.log(saludar('Ana'));",
    ].join('\n'),
  },
]

// Página provisional para probar el editor; la reemplaza el layout de la pantalla del problema (HU33).
function EditorPrueba() {
  const [lenguaje, setLenguaje] = useState(LENGUAJES[0].valor)
  const [codigo, setCodigo] = useState(LENGUAJES[0].ejemplo)

  function cambiarLenguaje(valor: string) {
    const elegido = LENGUAJES.find((opcion) => opcion.valor === valor)
    if (elegido) {
      setLenguaje(elegido.valor)
      setCodigo(elegido.ejemplo)
    }
  }

  return (
    <main>
      <h1>Editor de prueba</h1>
      <label htmlFor="lenguaje">Lenguaje</label>
      <select id="lenguaje" value={lenguaje} onChange={(evento) => cambiarLenguaje(evento.target.value)}>
        {LENGUAJES.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>
            {opcion.etiqueta}
          </option>
        ))}
      </select>
      <EditorCodigo valor={codigo} onCambio={setCodigo} lenguaje={lenguaje} />
      <pre>
        Caracteres: {codigo.length} · Líneas: {codigo.split('\n').length}
      </pre>
    </main>
  )
}

export default EditorPrueba
