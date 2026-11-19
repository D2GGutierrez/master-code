import { useEffect, useState } from 'react'
import { actualizarProblema, aprobarProblema, obtenerProblema } from '../api/problemas.js'
import { normalizarCodigoBase } from '../codigoBase.js'
import Aviso from '../components/Aviso.jsx'
import Formulario from '../components/Formulario.jsx'
import Icono from '../components/Icono.jsx'
import { Dibujo, EstadoVacio, Hero } from '../components/Pantalla.jsx'
import { faltanteDelEjercicio } from '../completitud.js'
import { camposDelProblema, erroresDelServidor } from './NuevoProblema.jsx'

// HU19: el docente corrige un problema (casi siempre uno generado por IA) y lo aprueba. «Guardar cambios» lo deja
// pendiente; «Guardar y aprobar» lo guarda y, si está completo, lo deja visible para los estudiantes.

const ACCIONES_PENDIENTE = [
  { valor: 'guardar', texto: 'Guardar cambios', secundaria: true },
  { valor: 'aprobar', texto: 'Guardar y aprobar' },
]
const ACCIONES_APROBADO = [{ valor: 'guardar', texto: 'Guardar cambios' }]

// Los valores con los que arranca el formulario, tal como los devuelve admin-api. Los casos y las alternativas traen
// su id de base de datos: sirve de llave estable en pantalla y no se envía.
function valoresDe(problema) {
  return {
    titulo: problema.titulo,
    enunciado: problema.enunciado,
    lenguaje: problema.lenguaje,
    dificultad: problema.dificultad,
    plataforma: problema.plataforma,
    tipo_ejercicio: problema.tipo_ejercicio,
    casos_prueba: problema.casos_prueba,
    codigo_base: problema.codigo_base ?? '',
    opciones: problema.opciones,
    respuesta_esperada: problema.respuesta_esperada ?? '',
    tema: problema.tema ?? '',
  }
}

function RevisarProblema({ id }) {
  // 'cargando' | 'listo' | 'no-existe' | 'error'
  const [estado, setEstado] = useState('cargando')
  const [problema, setProblema] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [errores, setErrores] = useState({})
  const [aviso, setAviso] = useState('')
  // Título del problema recién aprobado; vacío mientras no se aprueba.
  const [aprobado, setAprobado] = useState('')

  useEffect(() => {
    let activo = true
    async function cargar() {
      try {
        const { ok, status, cuerpo } = await obtenerProblema(id)
        if (!activo) {
          return
        }
        if (ok) {
          setProblema(cuerpo)
          setEstado('listo')
        } else {
          setEstado(status === 404 ? 'no-existe' : 'error')
        }
      } catch {
        if (activo) {
          setEstado('error')
        }
      }
    }
    cargar()
    return () => {
      activo = false
    }
  }, [id])

  const campos = camposDelProblema(null)

  async function guardar(valores, accion) {
    const quiereAprobar = accion === 'aprobar'
    const datos = { ...valores }
    // Solo se descarta un caso con entrada y salida vacías a la vez; los textos van sin recortar. Los casos que ya
    // existían conservan si son públicos u ocultos (es_publico), que esta pantalla no cambia.
    const enviados = (datos.casos_prueba || []).filter((caso) => caso.entrada !== '' || caso.salida_esperada !== '')
    if (datos.casos_prueba) {
      datos.casos_prueba = enviados.map(({ entrada, salida_esperada, es_publico }) => ({
        entrada,
        salida_esperada,
        ...(es_publico === undefined ? {} : { es_publico }),
      }))
    }
    if ('codigo_base' in datos) {
      datos.codigo_base = normalizarCodigoBase(datos.codigo_base)
    }
    if ('tema' in datos) {
      datos.tema = datos.tema.trim()
    }
    if (datos.tipo_ejercicio === 'opcion_multiple') {
      datos.opciones = (datos.opciones ?? []).map(({ texto, es_correcta }) => ({ texto: texto.trim(), es_correcta }))
    }
    if ('respuesta_esperada' in datos) {
      datos.respuesta_esperada = datos.respuesta_esperada.trim()
    }
    datos.enunciado = datos.enunciado ?? ''

    // Para aprobar el ejercicio debe estar completo: se avisa aquí, debajo del campo que falta, con la regla del servidor.
    if (quiereAprobar) {
      const falta = faltanteDelEjercicio({
        tipo: datos.tipo_ejercicio,
        enunciado: datos.enunciado,
        opciones: datos.opciones,
        respuestaEsperada: datos.respuesta_esperada,
      })
      if (falta) {
        setErrores({ [falta.campo]: falta.mensaje })
        return
      }
    }

    setEnviando(true)
    setErrores({})
    setAviso('')
    try {
      const guardado = await actualizarProblema(id, datos)
      if (!guardado.ok) {
        if (guardado.status === 404) {
          setEstado('no-existe')
        } else {
          const nuevos = erroresDelServidor(campos, guardado.cuerpo)
          // Los errores vienen en el orden de los casos enviados: se ubican en la posición que tiene cada uno en pantalla.
          if (Array.isArray(nuevos.casos_prueba)) {
            nuevos.casos_prueba = valores.casos_prueba.map((caso) => nuevos.casos_prueba[enviados.indexOf(caso)])
          }
          setErrores(nuevos)
        }
        return
      }
      setProblema(guardado.cuerpo)
      if (!quiereAprobar) {
        setAviso('Cambios guardados. El problema sigue pendiente de aprobación.')
        return
      }
      const resultado = await aprobarProblema(id)
      if (resultado.ok) {
        setProblema(resultado.cuerpo)
        setAprobado(resultado.cuerpo.titulo)
      } else if (resultado.status === 400) {
        // Lo guardado ya está en el servidor; lo que falta sale bajo su campo.
        const [campo, mensajes] = Object.entries(resultado.cuerpo)[0] ?? []
        setErrores({
          general: 'Se guardaron los cambios, pero el problema no se pudo aprobar.',
          ...(campo ? { [campo]: Array.isArray(mensajes) ? mensajes.join(' ') : String(mensajes) } : {}),
        })
      } else {
        setErrores({
          general: resultado.cuerpo.detail ?? 'Se guardaron los cambios, pero no se pudo aprobar. Inténtalo de nuevo.',
        })
      }
    } catch {
      // fetch solo lanza error cuando no hay conexión con el servidor.
      setErrores({ general: 'No se pudo guardar el problema. Inténtalo de nuevo.' })
    } finally {
      setEnviando(false)
    }
  }

  let contenido
  if (aprobado) {
    contenido = (
      <EstadoVacio ilustracion="exito" dibujo="celebracion">
        <p>Problema aprobado: {aprobado}.</p>
        <p>Ya lo pueden ver los estudiantes del curso.</p>
        <p>
          <a href="/revision">Volver a la bandeja</a> · <a href="/cursos">Ver mis cursos</a>
        </p>
      </EstadoVacio>
    )
  } else if (estado === 'cargando') {
    contenido = <p role="status">Cargando el problema…</p>
  } else if (estado === 'no-existe') {
    contenido = (
      <p>
        No encontramos ese problema. Puede que ya no exista o que sea de otro docente. <a href="/revision">Volver a la bandeja</a>.
      </p>
    )
  } else if (estado === 'error') {
    contenido = <p role="alert">No se pudo cargar el problema. Inténtalo de nuevo.</p>
  } else if (problema.archivado) {
    contenido = (
      <p>
        Este problema fue descartado y ya no se puede revisar. <a href="/revision">Volver a la bandeja</a>.
      </p>
    )
  } else {
    contenido = (
      <>
        <Aviso mensaje={aviso} />
        {problema.aprobado_por_docente && (
          <p className="campo-ayuda">Este problema ya está aprobado. Los cambios que guardes los verán los estudiantes.</p>
        )}
        <Formulario
          campos={campos}
          acciones={problema.aprobado_por_docente ? ACCIONES_APROBADO : ACCIONES_PENDIENTE}
          enviando={enviando}
          errores={errores}
          onEnviar={guardar}
          valoresIniciales={valoresDe(problema)}
        />
      </>
    )
  }

  const mostrarFormulario = !aprobado && estado === 'listo' && !problema.archivado

  return (
    <main>
      <Hero clase="hero-compacto" ilustracion="docente">
        <p className="hero-volver">
          <a href="/revision">
            <Icono nombre="atras" />
            Volver a la bandeja de revisión
          </a>
        </p>
        <h1>Revisar problema</h1>
        {estado === 'listo' ? (
          <p>
            {problema.generado_por_ia && <span className="distintivo-ia">Generado por IA</span>}{' '}
            {problema.curso_nombre} ·{' '}
            {problema.aprobado_por_docente ? 'Aprobado' : 'Pendiente de aprobación'}
          </p>
        ) : (
          <p>Corrige lo que haga falta y apruébalo para que lo vean tus estudiantes.</p>
        )}
      </Hero>
      {mostrarFormulario ? (
        <div className="problema-pagina">
          <div className="panel-formulario panel-formulario-ancho">{contenido}</div>
          <aside className="problema-consejos" aria-labelledby="consejos-titulo">
            <Dibujo nombre="equipo" className="problema-consejos-dibujo" />
            <h2 id="consejos-titulo">Qué revisar</h2>
            <ul>
              <li>
                <strong>Enunciado.</strong> Que se entienda solo y que el código de ejemplo, si lo hay, funcione.
              </li>
              <li>
                <strong>Corrección.</strong> La IA se equivoca a veces: comprueba que cada salida esperada, alternativa
                correcta o respuesta sea de verdad la correcta.
              </li>
              <li>
                <strong>Guardar cambios</strong> deja el problema pendiente en la bandeja.{' '}
                <strong>Guardar y aprobar</strong> lo deja visible para los estudiantes.
              </li>
            </ul>
          </aside>
        </div>
      ) : (
        <div className={aprobado ? undefined : 'panel-formulario panel-formulario-ancho'}>{contenido}</div>
      )}
    </main>
  )
}

export default RevisarProblema
