// Iconos lineales de un solo trazo, en el estilo de Lucide pero sin la dependencia. Son decorativos: el texto de al
// lado ya dice lo mismo, por eso van ocultos para los lectores de pantalla.
const TRAZOS = {
  flecha: 'M5 12h14M12 5l7 7-7 7',
  atras: 'M19 12H5M12 19l-7-7 7-7',
  mas: 'M12 5v14M5 12h14',
  correcto: 'M20 6 9 17l-5-5',
  libro: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14ZM20 17v4H6.5A2.5 2.5 0 0 1 4 19.5',
  codigo: 'M16 18l6-6-6-6M8 6l-6 6 6 6',
  salir: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
}

function Icono({ nombre, className }) {
  return (
    <svg className={className ? `icono ${className}` : 'icono'} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d={TRAZOS[nombre]} />
    </svg>
  )
}

export default Icono
