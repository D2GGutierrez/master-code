// Pruebas de completitud.js. No hace falta instalar nada: desde admin-web, `node --test`.
import assert from 'node:assert/strict'
import test from 'node:test'
import { contarHuecos, faltanteDelEjercicio } from './completitud.js'

const alt = (texto, es_correcta = false) => ({ texto, es_correcta })

test('una opción múltiple completa no tiene faltantes', () => {
  assert.equal(faltanteDelEjercicio({ tipo: 'opcion_multiple', opciones: [alt('4'), alt('5', true)] }), null)
})

test('opción múltiple: pocas o demasiadas alternativas, vacías, y sin o con dos correctas', () => {
  const faltante = (opciones) => faltanteDelEjercicio({ tipo: 'opcion_multiple', opciones })
  assert.equal(faltante(undefined).campo, 'opciones')
  assert.match(faltante([alt('a', true)]).mensaje, /entre 2 y 6/)
  assert.match(faltante(Array.from({ length: 7 }, (_, i) => alt(String(i), i === 0))).mensaje, /entre 2 y 6/)
  assert.match(faltante([alt('a', true), alt('  ')]).mensaje, /vacía/)
  assert.match(faltante([alt('a'), alt('b')]).mensaje, /exactamente una/)
  assert.match(faltante([alt('a', true), alt('b', true)]).mensaje, /exactamente una/)
})

test('predecir la salida exige la respuesta esperada', () => {
  assert.equal(faltanteDelEjercicio({ tipo: 'prediccion_salida', respuestaEsperada: ' 5 ' }), null)
  assert.equal(faltanteDelEjercicio({ tipo: 'prediccion_salida', respuestaEsperada: '  ' }).campo, 'respuesta_esperada')
  assert.equal(faltanteDelEjercicio({ tipo: 'prediccion_salida' }).campo, 'respuesta_esperada')
})

test('completar el espacio exige un solo hueco y la respuesta', () => {
  const base = { tipo: 'completar_espacio', respuestaEsperada: '+' }
  assert.equal(faltanteDelEjercicio({ ...base, enunciado: '<pre><code>print(2 ____ 3)</code></pre>' }), null)
  assert.equal(faltanteDelEjercicio({ ...base, enunciado: 'print(2 + 3)' }).campo, 'enunciado')
  assert.equal(faltanteDelEjercicio({ ...base, enunciado: 'a ___ b ___ c' }).campo, 'enunciado')
  // Dos guiones bajos (__init__) no son un hueco.
  assert.equal(faltanteDelEjercicio({ ...base, enunciado: '__init__ ___' }), null)
  assert.equal(faltanteDelEjercicio({ tipo: 'completar_espacio', enunciado: 'x ___' }).campo, 'respuesta_esperada')
  assert.equal(contarHuecos(undefined), 0)
})

test('escritura de código no se revisa aquí: la valida admin-api', () => {
  assert.equal(faltanteDelEjercicio({ tipo: 'escritura_codigo' }), null)
})
