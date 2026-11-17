# Puntos y cinturones

Esta es la regla de los puntos del cinturón (HU50-T4, CA-50.2 y CA-50.3). La decisión completa está en ADR-9, D18 (`docs/decisiones-tecnicas-adr.md`); aquí quedan los valores que se pueden consultar sin leer el código.

## Cómo se ganan los puntos

- Hay **un cinturón por estudiante y por lenguaje** (Python, Java o JavaScript). Los puntos de un ejercicio van al cinturón del lenguaje de ese ejercicio.
- Solo el **primer acierto de cada ejercicio** suma. Repetir un ejercicio ya resuelto, equivocarse o un envío parcial no dan puntos.
- Suman los aciertos de la web (ejercicios de código, cuando pasan todos los casos) y los del celular (predecir la salida, opción múltiple y completar el espacio), también dentro del Dojo.
- El cinturón nace con el primer acierto del estudiante en ese lenguaje: en blanco y con los puntos de ese ejercicio.

## Puntos por tipo de ejercicio

| Tipo de ejercicio | Puntos |
| :--- | ---: |
| `escritura_codigo` | 30 |
| `prediccion_salida` | 10 |
| `opcion_multiple` | 10 |
| `completar_espacio` | 10 |

Un tipo que no está en la tabla no da puntos. Los valores están en `PuntosPorEjercicio` (`user-api`), un solo lugar; la prueba `PuntosCinturonDocTest` comprueba que esta tabla y el código dicen lo mismo.

El ejercicio de código vale el triple porque pide escribir el programa completo; los tres del celular son de respuesta corta.

## Cuántos puntos pide cada cinturón

| Nivel | Cinturón | Puntos acumulados desde los que se tiene |
| ---: | :--- | ---: |
| 1 | blanco | 0 |
| 2 | amarillo | 100 |
| 3 | naranja | 250 |
| 4 | verde | 500 |
| 5 | azul | 900 |
| 6 | marron | 1400 |
| 7 | negro | 2000 |

- El nivel es **el más alto cuyo umbral ya se alcanzó**. Se calcula al sumar, puede subir más de un nivel de golpe y **nunca baja**.
- Con los puntos justos del umbral ya se tiene el cinturón (con 100 puntos ya es amarillo).
- El negro es el último: no tiene siguiente.
- Los umbrales viven en la tabla `app_nivel_cinturon` (migración V11), no en el código: cambiar uno es una migración nueva, y la app y el perfil leen los mismos valores.

## Cuántos ejercicios son

Para dar una idea de la escala (sin contar los que ya se resolvieron antes):

| Para llegar a | Solo ejercicios del celular (10 puntos) | Solo ejercicios de código (30 puntos) |
| :--- | ---: | ---: |
| amarillo (100) | 10 | 4 |
| naranja (250) | 25 | 9 |
| verde (500) | 50 | 17 |
| azul (900) | 90 | 30 |
| marron (1400) | 140 | 47 |
| negro (2000) | 200 | 67 |

## Qué falta para el siguiente cinturón (HU51-T1)

`GET /api/yo/cinturones` devuelve, por cada lenguaje en el que el estudiante ya acertó, su nivel, sus puntos y lo que le falta:

```json
{ "cinturones": [
  { "lenguaje": "python", "nivel": 2, "nombre": "amarillo", "puntos": 130,
    "siguiente": { "nivel": 3, "nombre": "naranja", "puntos_requeridos": 250 },
    "puntos_faltantes": 120 }
] }
```

Con el cinturón negro, `siguiente` y `puntos_faltantes` van en `null`: la app muestra el mensaje especial (HU51-T3) en lugar de la barra.
