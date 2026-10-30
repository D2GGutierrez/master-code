import { useRef, useState } from 'react'
import { enviarSolucion, type CasoCalificado, type EnvioCalificado } from '../api/envios.ts'
import Icono from './Icono.tsx'
import { Ilustracion } from './Ilustracion.tsx'
import './EnvioSolucion.css'

type Props = {
  problemaId: number
  codigo: string
}

// reposo: botón «Enviar solución»; confirmando: pregunta antes de enviar (CA-36.2); evaluando: botones bloqueados (CA-36.6).
type Fase = 'reposo' | 'confirmando' | 'evaluando'

const ETIQUETA_RESULTADO = { correcto: 'Correcto', parcial: 'Parcial', incorrecto: 'Incorrecto' } as const

const ERROR_GENERAL = 'No se pudo enviar tu solución. Inténtalo de nuevo.'
const SIN_CONEXION = 'No se pudo contactar al servidor. Revisa tu conexión e inténtalo de nuevo.'

const ESTADO_DEL_CASO = {
  error_compilacion: 'Tu código no compiló.',
  error_ejecucion: 'Tu programa falló al ejecutarse.',
  tiempo_agotado: 'Tu programa tardó demasiado y se detuvo.',
} as const

// 503 trae su propio texto («Tu envío no se contó…»); 404 también cubre «no estás inscrito», para no revelar qué existe.
function mensajeDeError(status: number, detail?: string) {
  if (status === 404) {
    return 'El problema no está disponible o no estás inscrito en su curso.'
  }
  if (status === 503) {
    return detail ?? 'El servicio no está disponible. Tu envío no se contó: inténtalo de nuevo en un momento.'
  }
  if (status === 400 || status === 409) {
    return detail ?? ERROR_GENERAL
  }
  return ERROR_GENERAL
}

// Hasta dos decimales y con punto, igual que lo calcula user-api: 100, 75 o 66.67.
function formatoPuntaje(puntaje: number) {
  return String(Math.round(puntaje * 100) / 100)
}

// Marca las líneas del texto que no coinciden con las del otro, sin contar los espacios del final (como la calificación).
function lineasMarcadas(texto: string, otro: string) {
  const mias = texto.split(/\r?\n/)
  const suyas = otro.split(/\r?\n/)
  return mias.map((linea, indice) => ({
    linea,
    difiere: linea.trimEnd() !== (suyas[indice] ?? '').trimEnd(),
  }))
}

function Texto({ texto, otro }: { texto: string; otro?: string }) {
  // Sin otro texto con el que comparar (el caso pasó) no se marca nada.
  const lineas = otro === undefined ? [{ linea: texto, difiere: false }] : lineasMarcadas(texto, otro)
  return (
    <pre>
      {lineas.map(({ linea, difiere }, indice) =>
        difiere ? <mark key={indice}>{linea || ' '}</mark> : <span key={indice}>{linea + '\n'}</span>,
      )}
    </pre>
  )
}

// Solo un caso público trae entrada y salidas; uno oculto no tiene nada más que pasó o falló (HU39).
function DetalleCaso({ caso }: { caso: CasoCalificado }) {
  const esperada = caso.salida_esperada ?? ''
  const obtenida = caso.salida_obtenida ?? ''
  const nota = caso.estado && caso.estado !== 'ok' ? ESTADO_DEL_CASO[caso.estado] : ''
  return (
    <details open={!caso.paso}>
      <summary>Ver detalle</summary>
      <p>Entrada</p>
      {caso.entrada ? <pre>{caso.entrada}</pre> : <p>(sin entrada)</p>}
      <p>Salida esperada</p>
      <Texto texto={esperada} otro={caso.paso ? undefined : obtenida} />
      <p>Tu salida</p>
      <Texto texto={obtenida} otro={caso.paso ? undefined : esperada} />
      {nota && <p className="envio-error">{nota}</p>}
      {caso.error && <pre className="envio-error">{caso.error}</pre>}
    </details>
  )
}

function Resultado({ envio }: { envio: EnvioCalificado }) {
  return (
    <>
      {envio.resultado === 'correcto' && <Ilustracion nombre="exito" className="envio-exito" />}
      <h2>Resultado: {ETIQUETA_RESULTADO[envio.resultado]}</h2>
      <p className="envio-resumen">
        Intento n.º {envio.numero_intento} · Puntaje: {formatoPuntaje(envio.puntaje)} % · Casos superados:{' '}
        {envio.casos_superados} de {envio.casos_total}
      </p>
      {envio.aviso && <p className="envio-error">{envio.aviso}</p>}
      <ol className="envio-casos">
        {envio.casos.map((caso, indice) => (
          <li key={indice} className={caso.paso ? 'envio-paso' : 'envio-falla'}>
            <Icono nombre={caso.paso ? 'correcto' : 'incorrecto'} />
            Caso {indice + 1}
            {caso.publico ? '' : ' (oculto)'}: {caso.paso ? 'pasó' : 'falló'}
            {caso.publico && <DetalleCaso caso={caso} />}
          </li>
        ))}
      </ol>
    </>
  )
}

// HU36-T3 a T5 y HU38-T3: enviar la solución como intento oficial y ver el resultado en el mismo lugar, sin recargar.
function EnvioSolucion({ problemaId, codigo }: Props) {
  const [fase, setFase] = useState<Fase>('reposo')
  // Con mensaje se muestra el mensaje; si no, el último resultado; si tampoco hay, el texto inicial.
  const [mensaje, setMensaje] = useState('')
  const [envio, setEnvio] = useState<EnvioCalificado | null>(null)
  // Un segundo clic en el mismo instante llega antes de que React deshabilite el botón: esto lo frena.
  const enCurso = useRef(false)

  function pedirConfirmacion() {
    if (!codigo.trim()) {
      setMensaje('Escribe tu código antes de enviarlo.')
      return
    }
    setMensaje('')
    setFase('confirmando')
  }

  async function enviar() {
    if (enCurso.current) {
      return
    }
    enCurso.current = true
    setFase('evaluando')
    setMensaje('')
    setEnvio(null)
    try {
      const respuesta = await enviarSolucion(problemaId, codigo)
      if (respuesta.ok) {
        setEnvio(respuesta.envio)
      } else {
        setMensaje(mensajeDeError(respuesta.status, respuesta.detail))
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setMensaje(SIN_CONEXION)
    } finally {
      enCurso.current = false
      setFase('reposo')
    }
  }

  return (
    <section className="envio" aria-label="Enviar solución">
      <h2>Enviar solución</h2>
      {fase === 'confirmando' ? (
        <div className="envio-confirmacion" role="group" aria-label="Confirmar envío">
          <p>¿Enviar tu solución? Se evaluará con todos los casos de prueba y contará como un intento oficial.</p>
          <button type="button" className="boton-primario" onClick={enviar}>
            Confirmar envío
          </button>{' '}
          <button type="button" onClick={() => setFase('reposo')}>
            Cancelar
          </button>
        </div>
      ) : (
        <button type="button" className="boton-primario" onClick={pedirConfirmacion} disabled={fase === 'evaluando'}>
          Enviar solución
        </button>
      )}
      <div role="region" aria-label="Resultado del envío" aria-live="polite">
        {fase === 'evaluando' ? (
          <p role="status">Evaluando tu solución…</p>
        ) : mensaje ? (
          <p className="envio-error">{mensaje}</p>
        ) : envio ? (
          <Resultado envio={envio} />
        ) : (
          <p>Aquí verás el resultado de tu envío.</p>
        )}
      </div>
    </section>
  )
}

export default EnvioSolucion
