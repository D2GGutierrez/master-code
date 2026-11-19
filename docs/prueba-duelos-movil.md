# Prueba de los duelos en dos celulares y con red limitada (HU58-T4, HU59-T5, HU60-T4)

Guía para Gael (con Juan Diego). Lo que se puede comprobar con código **ya está en pruebas automáticas**; esta guía cubre lo que solo se ve con dos pantallas reales: dos celulares jugando la misma partida y una conexión mala. Las celdas «Resultado» de la hoja del final se llenan al recorrerla.

Fecha: 22 de noviembre de 2026. Decisiones: ADR-15 (D26) y ADR-10 (D20). Criterios: CA-57.1 a CA-60.4 en `docs/historias-criterios.md`.

## Qué ya comprueban las pruebas automáticas

| Qué | Dónde | Cómo corre |
| :--- | :--- | :--- |
| Una partida completa entre dos cuentas (retar → invitación → aceptar → tres turnos → resultado → historial → ranking) | `PartidaDosCuentasApiTest` (user-api, HU58-T4) | `./mvnw test -Dtest=PartidaDosCuentasApiTest` |
| Ranking con empates jugados de verdad, estudiante sin partidas, reto pendiente o rechazado que no cuenta, no inscrito (403) y sin sesión (401) | `PartidaDosCuentasApiTest` y `PartidaHistorialRankingApiTest` (user-api, HU60-T4) | igual |
| Marcador con `ETag` y 304, rival conectado o no, abandono a los 30 s | `PartidaMarcadorApiTest` (user-api) | igual |
| La app: turno a turno, corrección, marcador en vivo, rival sin señal, abandono, retomar | `PartidaViewModelTest` (app) | `./gradlew testDebugUnitTest` |
| Conexión lenta o cortada con la pila real de la app (cliente HTTP, repositorio y ViewModel) contra un servidor que se ralentiza o corta: la respuesta del estudiante no espera al marcador lento, la partida sigue en pantalla con el aviso y el marcador se pone al día al volver la señal, y el `ETag` viaja de verdad | `PartidaConexionLentaTest` (app, HU59-T5) | igual |
| Lectura de cada ruta de duelos (`DuelosApiTest`), códigos de error → mensajes (`DuelosRepositoryTest`), retar, invitaciones, historial y ranking (`DuelosListasViewModelTest`) | app | igual |

Las pantallas Compose (`ui/duelos/`) **no tienen prueba automática**: se comprueban con esta guía.

## Antes de empezar

1. Servidores y datos de la demo según `docs/guia-demo.md` §1. El curso debe tener **al menos un ejercicio para celular** (`plataforma = movil`): sin eso, retar da 409.
2. Dos cuentas inscritas en el **mismo curso**: por ejemplo `ana.torres.demo` y `lucia.paredes.demo` (`docs/recorrido-aceptacion-movil.md` §1). Una en cada dispositivo.
3. Dos dispositivos: dos emuladores de Android Studio, o un emulador y un teléfono. Un teléfono físico necesita las direcciones del servidor de la PC (ver §1 punto 3 del recorrido).
4. Cada dispositivo con **su** cuenta. Si se usan dos emuladores, cada uno tiene su propio almacenamiento, así que no se pisan las sesiones.

## Parte A. La partida completa con dos dispositivos (HU58-T4)

Llamaremos **A** al dispositivo de Ana y **B** al de Lucía.

1. En A: «Mis cursos» → **Duelos con compañeros** → pestaña **Retar**. Si Ana tiene más de un curso, elegirlo. **Esperado:** aparece Lucía en la lista de compañeros (nunca Ana, y solo con su alias, no su correo).
2. En A: tocar **Retar a lucia…**. **Esperado:** se abre la partida con «Esperando a que lucia acepte tu reto…».
3. En B: «Mis cursos» → Duelos → pestaña **Invitaciones** → **Actualizar**. **Esperado:** «ana te retó» con **Aceptar** y **Rechazar**.
4. En B: **Aceptar**. **Esperado:** en B se abre la partida con el primer ejercicio; **en A, sin tocar nada, en 2 a 4 segundos** la espera se reemplaza por el mismo primer ejercicio (CA-58.1, CA-58.2).
5. Mirar el marcador de arriba en los dos: «Tú», el alias del rival, el puntaje de cada uno, «N de M respondidos» y «lucia está conectado» / «ana está conectado».
6. Jugar los turnos. Responder primero en A y mirar B: **Esperado:** el marcador de B muestra «1 de M respondidos» del rival a los pocos segundos, **sin que B haga nada** (CA-59.1, CA-59.2). Después responder en B y mirar A.
7. En el turno 2, comprobar que **los dos ven el mismo ejercicio** (mismo título y enunciado) (CA-58.2) y que no hay forma de saltarse un turno (CA-58.3, CA-58.4): tras responder, solo aparece «Siguiente ejercicio», que lleva al turno que sigue.
8. Probar los tres tipos de ejercicio si el curso los tiene (predecir la salida, completar el espacio, opción múltiple): se ven con las mismas pantallas del Dojo, pero **sin «Intentar de nuevo»** y sin intentos restantes.
9. Terminar los turnos en A y esperar. **Esperado:** A muestra «Terminaste tus ejercicios. Esperando a que lucia termine…» con el marcador en vivo.
10. Terminar en B. **Esperado:** en cuanto B responde el último, su pantalla muestra la corrección y **«Ver resultado»**; A pasa **sola** al resultado. Los dos ven «¡Ganaste!», «Perdiste» o «Empate» según el marcador final, y «Marcador final: X a Y».
11. En los dos: **Volver a Duelos** → **Historial**. **Esperado:** la partida con el rival, el resultado, el puntaje de cada uno y la fecha. → **Ranking** (con el curso elegido): Ana y Lucía aparecen, **la fila propia resaltada** y con «(tú)».

### Variantes

| Variante | Pasos | Esperado |
| :--- | :--- | :--- |
| Rechazo | En B: **Rechazar** la invitación | En A: «Tu rival rechazó el reto.» con **Volver a Duelos**. La invitación desaparece de B |
| Caducidad | Dejar un reto sin aceptar 24 h (o ver `PARTIDA_CADUCIDAD_RETO_HORAS`) | En A: «El reto caducó…». En B, aceptarlo da «Ese reto ya caducó o ya fue respondido.» y la invitación sale de la lista |
| Reto repetido | En A, retar otra vez a Lucía con la partida viva | «Ya hay un reto activo con ese compañero…» |
| Abandono (CA-59.3, CA-59.4) | En B, mitad de la partida: **cerrar la app de verdad** y esperar más de 30 s | En A: «lucia perdió la señal» a los ~10 s y, pasados 30 s, el resultado «¡Ganaste!» con «Tu rival abandonó la partida.». Al reabrir B y entrar a Historial: la partida figura perdida por abandono |
| App en segundo plano | En B, tocar el botón Inicio (sin cerrar la app) y esperar más de 30 s | Igual que cerrar la app: la app deja de dar señal en cuanto sale de la vista, así que A ve «lucia perdió la señal» y, pasados 30 s, gana por abandono |
| Volver a tiempo | En B: **Salir** (confirma) o apagar el Wi-Fi unos 15 s y volver | A ve «lucia perdió la señal» y, al volver B, se quita. La partida sigue |
| Retomar | En B, cerrar la app a mitad de partida (menos de 30 s) y abrir Duelos → Historial → **Continuar** en la partida en curso | Vuelve al **ejercicio del turno que le toca** (el mismo, no otro) y el marcador sigue donde estaba |
| Salir con la partida en juego | Tocar **Salir** o atrás | Pide confirmar: «Si sales y no vuelves en 30 segundos, perderás la partida por abandono.» |

## Parte B. Con conexión lenta o mala (HU59-T5, CA-59.5)

Para recortar la red de un dispositivo:

- **Emulador de Android Studio:** *Extended controls* (los tres puntos) → **Cellular** → *Network type* **EDGE** o **GPRS**, y *Signal strength* **Poor**. Para cortarla del todo: **Airplane mode** desde el panel de ajustes rápidos.
- **Teléfono físico:** conectarlo a una red con poca señal, o activar el modo avión y volver a activar el Wi-Fi a mitad de una acción. En Android también sirve alejarse del router.

Con la red limitada en **A** (B normal), repetir de la Parte A desde el paso 4 y comprobar:

| Qué | Esperado |
| :--- | :--- |
| Marcador con red lenta (CA-59.5) | El marcador de A **se sigue actualizando**, más despacio. La pantalla no se congela ni se cierra, y el ejercicio se puede contestar mientras tanto |
| Se corta la señal de A | Aparece el aviso rojo **«Conexión lenta o sin señal. Seguimos intentando…»**. El ejercicio y el último marcador **siguen en pantalla**. B ve «ana perdió la señal» a los ~10 s |
| Vuelve la señal en A antes de 30 s | El aviso rojo desaparece solo y el marcador se pone al día. Nadie pierde la partida |
| Responder sin señal | Debajo del ejercicio: «No se pudo enviar tu respuesta. Revisa tu conexión y vuelve a enviarla.» El texto escrito **no se pierde** y el botón **Enviar** sigue activo. Al volver la señal, **Enviar** funciona |
| La respuesta sí llegó pero la contestación se perdió | Al reenviar: «Ese turno ya estaba respondido. Pasamos al que sigue.» y aparece el ejercicio que sigue, sin sumar dos veces el punto |
| Sin señal más de 30 s | Se pierde la partida por abandono (CA-59.4): al volver, el resultado «Perdiste» con «Abandonaste la partida.» |

Anotar siempre: la red que se usó (tipo y calidad), cuánto tardó el marcador en actualizarse y si algo se congeló.

## Parte C. Ranking y casos de borde (HU60-T4)

| Caso | Pasos | Esperado |
| :--- | :--- | :--- |
| Sin partidas | Con `jorge.salas.demo` (solo inscrito): Duelos → **Ranking** | Se ve el ranking del curso con Jorge al final, en ceros y resaltado, y el aviso «Todavía no jugaste en este curso. Reta a un compañero para entrar al ranking.» |
| Historial vacío | Con Jorge: **Historial** | «Todavía no has jugado ninguna partida.» |
| Empate | Jugar una partida en la que los dos acierten lo mismo | «Empate» en los dos resultados; en el ranking, **una empatada**, ninguna ganada ni perdida |
| Mismos números | Dos estudiantes con la misma cuenta de ganadas, porcentaje y puntos | Siempre salen en el mismo orden (el de menor id primero) |
| Mi fila en otra página | Con más de 20 estudiantes inscritos (o `tamano` chico), abrir el ranking con alguien que no esté en las 20 primeras | Debajo de la lista aparece **«Tu puesto»** con su posición, aunque no esté cargada su página. **Ver más** trae la siguiente |
| No inscrito (CA-60.4) | Con una cuenta no inscrita en el curso, `GET /api/cursos/<id>/ranking` (por ejemplo con `curl` y su token) | **403**. En la app, un curso al que ya no pertenece da «Ya no estás inscrito en este curso.» |
| Sin sesión | La sesión vence con la pantalla abierta | La app vuelve al inicio de sesión |

## Hoja de resultados

| Dato | Valor |
| :--- | :--- |
| Quién recorrió | |
| Fecha | |
| Dispositivos (emulador o teléfono, versión de Android) | |
| Red limitada usada | |
| Commit de la app | |

| Parte | Resultado (✔ / ✘ y nota) |
| :--- | :--- |
| A. Partida completa con dos dispositivos | |
| A. Variantes (rechazo, caducidad, abandono, retomar) | |
| B. Conexión lenta y sin señal | |
| C. Ranking y casos de borde | |

Si algo falla con ✘, anotar el paso, qué se esperaba y qué pasó, con el dispositivo y la red.
