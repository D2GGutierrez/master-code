# Recorrido de aceptación de la app móvil (C2)

Guía para recorrer en el celular los criterios CA-45.x en adelante con los datos de la demo, y anotar lo que falle. La hace Gael con Juan Diego. **Este documento es la guía y la hoja de resultados: las columnas «Resultado» están vacías a propósito, se llenan al recorrer la app.** Lo que sí está lleno es la columna «Ya cubierto por pruebas», que dice qué criterios ya los comprueba una prueba automática (JVM o `user-api`); esas pruebas no reemplazan el recorrido, lo acortan: en el celular se confirma lo que ve y toca el estudiante.

Fecha de la guía: 22 de noviembre de 2026. Criterios: `docs/historias-criterios.md`. Tareas: `docs/historias-tareas.md`.

## 1. Antes de empezar

1. **Servidores y datos de la demo.** Seguir `docs/guia-demo.md` §1: `docker compose up -d --build`, Piston (`SANDBOX_PROVIDER=piston`), y `DEMO_CLAVE=<tu clave> make datos-demo` (no se guarda ni se imprime; la clave la pone quien lo corre). Al final el script imprime las cuentas.
2. **Cuentas con datos del celular (C1).**

   | Cuenta | Qué tiene | Para qué sirve |
   | :--- | :--- | :--- |
   | `ana.torres.demo` | Python: 6 respuestas del Dojo (acierta 5, falla 1), racha 5 (máxima 9), cinturón blanco con puntos y nivel guardado | Cuenta «con historial» |
   | `luis.ramirez.demo` | Python: 3 respuestas (falla 2, acierta 1), racha 1 | Cuenta que falla |
   | `lucia.paredes.demo` | Python: 5 respuestas (3 aciertos y 2 fallos), racha 3 | Cuenta intermedia |
   | `jorge.salas.demo` | Solo inscrito: sin intentos, sin racha, sin cinturón | Estudiante nuevo (CA-54.4) |

   Cada letra de las respuestas es un ejercicio del Dojo (A = acierto, E = error) y el script se detiene antes si el curso tiene menos ejercicios del celular. Las sesiones que sembró ya están **terminadas**, así que cada cuenta empieza el recorrido sin sesión pendiente. Si el script cambió los perfiles (`MOVIL` en `infra/scripts/datos_demo.py`), mandan los del script.
3. **La app.** Abrir `mobile/` en Android Studio y correrla en el **emulador**. La versión de depuración solo habla con `10.0.2.2` (la PC vista desde el emulador) y solo permite HTTP sin cifrar para esa dirección (`app/src/debug/res/xml/network_security_config.xml`). **En un teléfono físico no conecta** sin cambiar esas dos cosas (las direcciones de `app/build.gradle.kts` y la configuración de red de depuración); ese cambio no se hizo aquí porque afecta al proyecto y lo decide el equipo. Si el recorrido se hace en un teléfono real, anotar cómo se conectó.
4. **Si algo no conecta:** `docs/guia-demo.md` §6.

## 2. Qué se puede recorrer hoy y qué no

La app de hoy tiene: inicio de sesión, «Mis cursos», el Dojo (con las tres pantallas de ejercicio, continuar la sesión, «Terminar sesión» y un resumen mínimo) y «Avisos de práctica». **No tiene pantalla** (así que esos criterios no se pueden recorrer en el celular todavía, y la entrega debe decir que no se cumplen):

- Racha en la pantalla principal (CA-49.3, HU49-T3).
- Cinturón con su imagen en el perfil, barra de avance y mensaje del negro (CA-50.4, CA-51.2, CA-51.3, CA-51.4; HU50-T3, HU51-T2, HU51-T3). Los puntos y el cálculo sí existen en el servidor.
- Resumen completo de la sesión con los temas más fallados (CA-56.2; HU56-T2). Hoy el Dojo muestra solo aciertos, respondidos y puntos.
- Todo lo de duelos (CA-57.1 a CA-59.5; HU57 a HU59): en el servidor solo existe la tabla de partidas (V18).
- Avisos push que de verdad lleguen (CA-52.1, CA-52.2): falta el proyecto de Firebase (H5), las dependencias y HU52-T2 (el aviso diario desde el servidor). Ver `docs/notificaciones-push.md`.
- Un ejercicio fuera del Dojo (abrir un curso y entrar a uno): «Mis cursos» todavía no abre el curso (HU29). Por eso **todos los ejercicios se recorren dentro del Dojo.**

Para lo que existe en el servidor pero no tiene pantalla, se puede comprobar con la API (con el token de un estudiante, en `$TOKEN`): `GET /api/yo/racha`, `GET /api/yo/cinturones`.

## 3. Cómo anotar

En cada fila: **✔** si se cumple, **✘** si falla (y qué pasó, con el paso en que ocurrió), **—** si no se pudo probar (y por qué). Un criterio que falla se anota aquí y, si es un error, en una incidencia.

## 4. Ejercicios del celular (HU45 a HU48)

Entrar con una cuenta de estudiante → «Mis cursos» → **Practicar en el Dojo** → elegir **Python**. El Dojo entrega los ejercicios de uno en uno; si no sale el tipo que se busca, terminar la sesión y empezar otra.

| Criterio | Pasos en el celular | Resultado esperado | Ya cubierto por pruebas | Resultado |
| :--- | :--- | :--- | :--- | :--- |
| CA-45.1 | Ejercicio de **predecir la salida** | Se ve el fragmento de código dentro del enunciado | `PartesDelEnunciadoTest`, `EjercicioViewModelTest` | |
| CA-45.2 | Escribir una salida en el campo | Se puede escribir y enviar | `EjercicioViewModelTest` | |
| CA-45.3 y CA-45.4 | Enviar la salida correcta con espacios de más y en mayúsculas; luego una incorrecta | La primera cuenta como correcta, la segunda como incorrecta | `CorrectorPrediccionSalidaTest`, `RespuestasEquivalentesTest`, `RespuestaMovilApiTest` | |
| CA-45.5 | Fallar el mismo ejercicio hasta agotar los intentos (fuera del Dojo hay 3; dentro del Dojo cada ejercicio se responde una vez, ADR-12 D22) | En el Dojo no hay «Intentar de nuevo»; con 3 intentos gastados el servidor responde 409 | `IntentosMovilApiTest` | |
| CA-46.1 | Ejercicio de **opción múltiple** | Se ven todas las alternativas | `AlternativasMovilApiTest` | |
| CA-46.2 | Tocar una alternativa | Queda marcada («Elegida») y se puede enviar | `EjercicioViewModelTest` | |
| CA-46.3 | Ver el mismo ejercicio en dos sesiones distintas | Las alternativas pueden salir en otro orden (puede coincidir por azar: repetir) | `AlternativasMovilApiTest` | |
| CA-46.4 | — (no se ve en el celular) | La respuesta correcta no viaja antes de responder | `AlternativaCorrectaNoViajaTest` | |
| CA-46.5 | Mirar las alternativas con «Mostrar límites del diseño» (opciones de desarrollador) o con Accessibility Scanner | Cada botón mide al menos 48 dp de alto | — (visual) | |
| CA-47.1 y CA-47.2 | Ejercicio de **completar el espacio** | El espacio se distingue en el código y se escribe en él | `PartesDelEnunciadoTest`, `EjercicioViewModelTest` | |
| CA-47.3 y CA-47.4 | Escribir la respuesta con espacios de sobra y con otra mayúscula; luego una equivalente escrita distinto | Se aceptan según las reglas | `RespuestasEquivalentesTest`, `CorrectorDeEjerciciosTest` | |
| CA-48.1 | — (no se ve en el celular) | Los ejercicios móviles no usan el sandbox | `EjerciciosMovilesSinSandboxTest` | |
| CA-48.2 | Enviar una respuesta | Dice al instante si fue correcta | `RespuestaMovilApiTest`, `EjercicioEnvioTest` | |
| CA-48.3 | Enviar una respuesta incorrecta | Muestra cuál era la respuesta correcta | `RespuestaMovilApiTest`, `EjercicioEnvioTest` | |
| CA-48.4 y CA-48.5 | **Prueba del modo avión (HU48-T4):** ver §8 | Sin señal la respuesta se guarda; al volver la señal se envía sola y se registra **una sola vez** | `ModoAvionRespuestasTest`, `EnviadorDeRespuestasTest`, `IntentosMovilApiTest` | |

## 5. Racha, cinturón y avisos (HU49 a HU52)

| Criterio | Pasos | Resultado esperado | Ya cubierto por pruebas | Resultado |
| :--- | :--- | :--- | :--- | :--- |
| CA-49.1 | Con `jorge.salas.demo`, acertar un ejercicio en el Dojo y pedir `GET /api/yo/racha` | `racha_actual` pasa a 1 y `practico_hoy` es verdadero | `RachaApiTest`, `DojoRachasApiTest` | |
| CA-49.2 | Con el teléfono en otra zona horaria, abrir la app y acertar a una hora cercana a medianoche | El día que cuenta es el del estudiante, no el del servidor | `ZonaHorariaApiTest`, pruebas de `racha/` | |
| CA-49.3 | — | **Sin pantalla (HU49-T3)** | — | |
| CA-49.4 | Con `luis.ramirez.demo` (racha viva), esperar un día sin acertar o ajustar la fecha del dato de prueba y pedir `GET /api/yo/racha` | Con un día completo sin acierto, la racha actual es 0 | pruebas de `racha/` | |
| CA-50.1 a CA-50.3 | Acertar ejercicios de cada tipo y pedir `GET /api/yo/cinturones` | Suman 30 puntos (código) o 10 (los otros tres); el nivel sube justo en cada umbral; repetir un ejercicio no suma | `CinturonServicioTest`, `CinturonSubidaDeNivelTest`, `PuntosCinturonDocTest` | |
| CA-50.4 | — | **Sin pantalla (HU50-T3):** las imágenes de los siete cinturones existen pero ninguna pantalla las muestra | — | |
| CA-51.1 | `GET /api/yo/cinturones` con `ana.torres.demo` y con `jorge.salas.demo` | Ana: cinturón, `puntos_faltantes` y `siguiente`; Jorge: lista vacía | `CinturonesApiTest`, `CinturonesPorNivelApiTest` (cada nivel y a un punto de cada umbral) | |
| CA-51.2 a CA-51.4 | — | **Sin pantalla (HU51-T2, HU51-T3)** | `CinturonesPorNivelApiTest` cubre el negro (`siguiente` y `puntos_faltantes` en `null`) | |
| CA-52.1 | — | **Falta HU52-T2** (aviso diario desde el servidor) | — | |
| CA-52.2 | Instalar con Firebase y pedir un mensaje de prueba (`docs/notificaciones-push.md`) | Llega la notificación | `DispositivoApiTest`, `DispositivoRepositoryTest` (solo el registro del token; **no** la llegada) | |
| CA-52.3 | «Mis cursos» → **Avisos de práctica** → apagar el interruptor | Se apaga, y al salir y volver sigue apagado. Desaparece la hora | `AvisosViewModelTest` | |
| CA-52.4 | Mismo lugar → **Cambiar la hora** → elegir 07:30 | Se ve «Te avisamos a las 07:30» y se conserva al cerrar y abrir la app. Por defecto es 20:00 | `AvisosViewModelTest` | |
| Permiso (Android 13 o más) | Primera vez que se abre «Avisos de práctica», o al volver a activar el interruptor | El sistema pide el permiso de notificaciones; si se niega, los avisos quedan apagados | — (solo en el celular) | |

Hay que recordar que los ajustes de avisos viven **solo en el teléfono** (ADR-14): apagarlos no detiene un aviso que mande el servidor hasta que HU52-T2 los lea.

## 6. Dojo (HU53 a HU56)

| Criterio | Pasos | Resultado esperado | Ya cubierto por pruebas | Resultado |
| :--- | :--- | :--- | :--- | :--- |
| CA-53.1 | «Practicar en el Dojo» → Python | Se abre la sesión y aparece el primer ejercicio | `DojoApiTest`, `DojoViewModelTest` | |
| CA-53.2 | Responder y tocar **Siguiente ejercicio** | Aparece otro ejercicio sin pedirlo aparte | `DojoApiTest`, `DojoViewModelTest` | |
| CA-53.3 | Hacer una sesión larga | No se repite un ejercicio dentro de la misma sesión; cuando no quedan, dice «No quedan más ejercicios» | `DojoApiTest` | |
| CA-53.4 | Acertar y fallar | El contador «Aciertos: N» sube solo con los aciertos | `DojoViewModelTest` | |
| CA-54.1 y CA-54.2 | Con `jorge.salas.demo`, acertar dos seguidos y luego fallar dos seguidos | Tras dos aciertos salen ejercicios más difíciles; tras dos errores, más fáciles | `ReglaDificultadTest`, `DojoDificultadApiTest` | |
| CA-54.3 | Terminar la sesión con «Terminar sesión» y abrir otra | La nueva parte del nivel alcanzado | `DojoDificultadApiTest` | |
| CA-54.4 | Con `jorge.salas.demo` (nuevo), primera sesión | Parte del nivel fácil | `DojoDificultadApiTest`, `NivelDificultadMigracionTest` | |
| CA-54.5 | — | La regla responde a secuencias de aciertos y de errores | `ReglaDificultadTest` | |
| CA-55.1 | En un ejercicio, salir con atrás | Queda guardado cuál era el ejercicio y los aciertos | `DojoViewModelTest` | |
| CA-55.2 | Después de salir con atrás, volver a «Practicar en el Dojo» | Se ofrece **Continuar** («Dejaste una sesión de Python a medias, con N aciertos de M respondidos»). Al continuar sale **el mismo ejercicio** que se dejó, con los aciertos de antes. **Empezar de nuevo** termina la anterior y pide el lenguaje | `DojoReanudarApiTest`, `DojoReanudarViewModelTest` | |
| CA-55.3 | Dentro de la sesión | Está el botón **Terminar sesión** | `DojoReanudarViewModelTest` | |
| CA-55.4 | Tocar **Terminar sesión** | Pregunta «¿Terminar la sesión?» con **Terminar** y **Seguir practicando**; «Seguir practicando» no cambia nada | `DojoReanudarViewModelTest` | |
| CA-55.5 | **Cerrar la app a media sesión (HU55-T4):** ver §8 | Al abrirla de nuevo se ofrece continuar donde se quedó | `DojoReanudarViewModelTest` (cerrar y reabrir) | |
| CA-56.1 | Terminar la sesión | El resumen muestra los aciertos de los respondidos | `DojoResumenApiTest`, `DojoReanudarViewModelTest` | |
| CA-56.2 | — | **Sin pantalla (HU56-T2):** los temas más fallados los trae el servidor pero la app aún no los muestra | `DojoResumenApiTest` | |
| CA-56.3 | Terminar una sesión con aciertos | El resumen dice los puntos sumados; `GET /api/yo/cinturones` los refleja | `DojoResumenApiTest`, `CinturonServicioTest` | |
| CA-56.4 | Una sesión en la que se acierta todo | Resumen con todos los aciertos y sin temas a repasar | `DojoResumenApiTest` (sesión perfecta) | |
| CA-56.5 | Una sesión en la que se falla todo | Resumen con 0 aciertos y 0 puntos, sin cinturón nuevo | `DojoResumenApiTest` (sesión sin aciertos) | |

## 7. Duelos (HU57 a HU59)

No hay pantalla ni rutas (solo la tabla de partidas, V18). Todos los criterios CA-57.1 a CA-59.5 se anotan como **—: sin implementar**.

## 8. Las dos pruebas que no se pueden hacer sin teléfono

### HU48-T4: modo avión, la respuesta se envía una sola vez

1. Entrar con `ana.torres.demo` → Dojo → Python, hasta tener un ejercicio en pantalla.
2. Activar el **modo avión** (en el emulador: panel de ajustes rápidos, o el panel *Extended controls → Cellular/Wi-Fi*).
3. Responder el ejercicio. **Esperado:** la pantalla dice «Sin conexión. Tu respuesta quedó guardada en el teléfono y se enviará cuando vuelva la señal.» (no un error, no se pierde el texto).
4. Desactivar el modo avión. **Esperado:** en unos segundos la pantalla pasa sola de «guardada» al resultado (correcto o incorrecto).
5. Comprobar que se registró **una sola vez**: en `user-api`, `SELECT count(*) FROM app_schema.app_envio WHERE estudiante_id = <id de Ana> AND problema_id = <el ejercicio>;` debe subir en **1**, no en 2, y los puntos del cinturón no se suman otra vez.
6. Variante: con la respuesta guardada, **cerrar la app** y abrirla con señal. **Esperado:** se envía al abrir.

Lo que ya prueba `ModoAvionRespuestasTest` (con el cliente HTTP de verdad y un servidor que cuenta por `intento_id`): sin señal se guarda y nada llega al servidor; al volver se envía una vez con el mismo `intento_id`; avisos de señal repetidos no reenvían; y si la respuesta llegó pero la señal se cortó antes de contestar, el reenvío no se registra dos veces.

### HU55-T4: cerrar la app a mitad de sesión y volver a abrirla

1. Entrar → Dojo → Python → responder un ejercicio (que cuente un acierto) y dejar el siguiente **sin responder**.
2. Cerrar la app **de verdad**: desde recientes, deslizarla; o `adb shell am force-stop com.mastercode.app`.
3. Abrir la app. **Esperado:** «Mis cursos» (la sesión de la cuenta se conserva) → **Practicar en el Dojo** → «Sesión sin terminar» con los aciertos y respondidos de antes.
4. Tocar **Continuar**. **Esperado:** vuelve el ejercicio que se dejó sin responder (el mismo, no otro) y el contador de aciertos sigue donde estaba.
5. Repetir cerrando la app **justo después de responder**: al continuar, aparece un ejercicio nuevo.
6. Repetir cerrando la app con **sesión sin conexión**: la oferta aparece igual, y «Continuar» avisa que no hay conexión y se puede reintentar al volver la señal.
7. Probar «Terminar sesión» → **Terminar** y reabrir la app: **no** se ofrece nada.

## 9. Hoja de resultados

| Dato | Valor |
| :--- | :--- |
| Quién recorrió | |
| Fecha | |
| Dispositivo y versión de Android (emulador o teléfono) | |
| Commit de la app | |
| Criterios con ✔ | |
| Criterios con ✘ (con la nota de qué pasó) | |
| Criterios sin probar (—) y por qué | |

La entrega dice qué criterios **no** se cumplen: los de §2 (sin pantalla) y los que salgan con ✘ en esta hoja. El hito dice que este recorrido cierra el MVP.
