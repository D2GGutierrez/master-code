// Iconos lineales de un solo trazo, en el estilo de Lucide pero sin la dependencia. Son decorativos: el texto de al
// lado ya dice lo mismo, por eso van ocultos para los lectores de pantalla.
const TRAZOS = {
  flecha: 'M5 12h14M12 5l7 7-7 7',
  atras: 'M19 12H5M12 19l-7-7 7-7',
  correcto: 'M20 6 9 17l-5-5',
  incorrecto: 'M18 6 6 18M6 6l12 12',
  buscar: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16ZM21 21l-4.3-4.3',
  salir: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  codigo: 'M16 18l6-6-6-6M8 6l-6 6 6 6',
  premio: 'M12 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12ZM8.2 13.9 7 22l5-3 5 3-1.2-8.1',
  rayo: 'M13 2 3 14h9l-1 8 10-12h-9l1-8z',
  libro: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14ZM20 17v4H6.5A2.5 2.5 0 0 1 4 19.5',
  pantalla: 'M2 3h20v14H2zM8 21h8M12 17v4',
  movil: 'M5 2h14v20H5zM12 18h.01',
  circulo: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z',
  media: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20ZM12 2v20',
}

function Icono({ nombre }: { nombre: keyof typeof TRAZOS }) {
  return (
    <svg className="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d={TRAZOS[nombre]} />
    </svg>
  )
}

export default Icono
