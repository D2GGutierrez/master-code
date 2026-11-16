# Regla de dificultad del Dojo (HU54-T1)

Cómo sube o baja la dificultad de los ejercicios de una sesión del Dojo según los aciertos del estudiante (HU-54, CA-54.1). Es la especificación que implementa HU54-T2 y que prueba HU54-T5; si el código y este documento no coinciden, se corrige uno de los dos en el mismo commit.

## La regla

**Dos aciertos seguidos suben un nivel y dos errores seguidos lo bajan.**

Los niveles son los tres que ya usan los problemas (campo `dificultad`, ADR de HU13): `facil`, `medio` y `dificil`, en ese orden. El nivel de la sesión es siempre uno de los tres.

| Antes de responder | Respuesta | Después |
| :--- | :--- | :--- |
| racha de aciertos 1 | acierto | **sube un nivel**; las dos rachas vuelven a 0 |
| racha de errores 1 | error | **baja un nivel**; las dos rachas vuelven a 0 |
| cualquier racha | acierto | racha de aciertos + 1; **la racha de errores vuelve a 0** |
| cualquier racha | error | racha de errores + 1; **la racha de aciertos vuelve a 0** |

Detalles que la regla fija:

- **«Seguidos» significa sin una respuesta contraria en medio.** Acierto, error, acierto no suma dos aciertos: cada cambio de signo reinicia la otra racha.
- **Después de cambiar de nivel se parte de cero.** Hacen falta otros dos aciertos (o dos errores) para el siguiente cambio; así el nivel no oscila con cada respuesta.
- **Los extremos no se pasan.** En `dificil`, dos aciertos seguidos no suben nada; en `facil`, dos errores seguidos no bajan nada. El nivel se queda igual y las rachas vuelven a 0.
- **Qué cuenta como respuesta.** Solo una respuesta que `user-api` corrigió como `correcto` o `incorrecto` (`POST /api/problemas/{id}/respuesta`, D15). Una respuesta inválida (vacía, demasiado larga, de otro ejercicio) y un ejercicio que se deja sin responder no cuentan ni como acierto ni como error y no tocan las rachas.
- **Cada ejercicio cuenta una vez.** En una sesión no se repite ejercicio (HU53-T2, D19), así que no hay reintentos que contar.

### Ejemplos

Se empieza en `medio`. A = acierto, E = error.

| Respuestas | Nivel después de cada una |
| :--- | :--- |
| A A | medio → medio → **dificil** |
| A A A A | medio → medio → dificil → dificil → dificil (en el tope se queda) |
| E E | medio → medio → **facil** |
| E E E E | medio, medio, facil, facil, facil (en el piso se queda) |
| A E A E A E | medio, siempre: nunca hay dos seguidos |
| A A E A A | medio → medio → dificil → dificil → dificil → dificil (el error aislado solo reinicia las rachas) |
| A A E E | medio → dificil, y luego dificil → dificil → **medio** |

## Qué ejercicio sigue (HU54-T2, CA-54.2)

El siguiente ejercicio de la sesión se elige **entre los del nivel resultante** (campo `dificultad` del problema), con las demás condiciones de D19 (móvil, nunca `escritura_codigo`, del lenguaje de la sesión, visible en un curso del estudiante, sin repetir en la sesión, al azar).

Si en el nivel resultante ya no queda ningún ejercicio sin entregar, se toma el **nivel más cercano** que tenga alguno; si hay dos a la misma distancia se prefiere el más fácil. Si en ningún nivel queda alguno, la sesión responde 204 «no quedan ejercicios», como hoy. El nivel de la sesión no cambia por esta sustitución: lo cambia solo la regla.

## Lo que fijan las otras tareas

| Tarea | Qué fija | Estado de este documento |
| :--- | :--- | :--- |
| HU54-T3 | Al terminar la sesión se guarda el nivel alcanzado (CA-54.3). | Pendiente: el lugar donde se guarda lo decide esa tarea. |
| HU54-T4 | El nivel inicial de un estudiante nuevo (CA-54.4). | Pendiente. **Propuesta:** `facil`, para que el primer ejercicio nunca frustre; quien no es nuevo parte del nivel guardado en HU54-T3. Se confirma al hacer HU54-T4 y se escribe aquí. |

## Dónde vive el código

La regla es una función pura de `user-api` (sin base de datos ni reloj): recibe el nivel, las dos rachas y si la respuesta fue acierto, y devuelve el nivel y las rachas nuevas. Así HU54-T5 puede probarla con rachas largas de aciertos y de errores sin levantar nada (CA-54.5). Las rachas de la sesión se guardan junto al nivel, para que sobrevivan si la app se cierra y se reanuda (HU55).

## Cómo probarla (HU54-T5)

La prueba debe cubrir como mínimo: dos aciertos suben, dos errores bajan, acierto-error alternados no cambian nada, el error aislado reinicia la racha de aciertos, el tope en `dificil`, el piso en `facil`, que después de subir hacen falta dos aciertos más, y una respuesta inválida que no toca las rachas.
