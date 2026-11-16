# Roadmap — Sprint 4 · MASTER CODE

**Sprint:** 16 – 27 de noviembre de 2026 · **Último sprint del proyecto** · **Hito:** app móvil completa estilo Consoly; cierra el MVP

**Estado de partida:** viernes 20 de noviembre, día 5 de 10. El historial de Git no tiene commits después del 8 de noviembre en ninguna rama (último: `f6f5f13`, HU35-T4), así que **ninguna tarea del Sprint 4 está hecha** salvo HU46-T1, que ya estaba en `main` (sección 1). Si hay trabajo sin subir, hay que avisar y se recalcula. El Sprint 3 cerró con 62 de sus 69 tareas; las 7 que faltan se arrastran a este plan.

**Plan:** 16 historias (HU45 a HU60), 66 tareas, 90 h, tomadas de `docs/historias-tareas.md`. **Quedan 65 tareas y 89 h** del Sprint 4, más 7 tareas arrastradas del Sprint 3 (7 h), 9 huecos que ninguna tarea cubre (15 h) y 4 piezas de cierre del proyecto (6 h, ambas estimaciones), hasta **85 piezas y 117 h**. Se agregan 3 decisiones sin horas (D12 a D14, sección 6). Las dependencias marcadas con \* en las tablas no están en `historias-tareas.md`: las agrega este roadmap.

---

## 1. Qué ya está hecho

**Del Sprint 3 queda esto** (todas con sus dependencias cumplidas, así que pueden hacerse hoy):

| Tarea | Qué falta | Quién |
|---|---|---|
| **HU63-T3** | Mostrar al docente el total de puntos de la evaluación | Carlos |
| **HU33-T5** | Probar el layout del editor en 1366 px y en 1920 px | Carlos (con Gael) |
| **HU35-T5** | Prueba: al reabrir el editor aparece lo último que se escribió | Carlos (con Gael, Juan Diego) |
| **HU36-T6** | Prueba a mano del flujo completo: escribir, enviar y ver el resultado | Carlos (con Gael, Juan Diego) |
| **HU40-T5** | Prueba del error de sintaxis (falta esa de las pruebas de errores) | Gael (con Juan Diego) |
| **HU63-T4** | Pruebas de la suma de puntos y de que no haya puntos negativos | Gael (con Juan Diego) |
| **HU64-T4** | Pruebas del cierre del intento por tiempo | Gael (con Juan Diego) |

**Del Sprint 4 hay una tarea hecha y una a medias:**

| Tarea | Estado | Evidencia |
|---|---|---|
| **HU46-T1** Modelo de alternativas | Hecha (1 h) | `OpcionRespuesta` en `admin-api/problemas/models.py` (tabla `admin_opcion_respuesta`: problema, texto, es_correcta), migración `0007_campos_de_ia_y_opciones`, validación de «exactamente una correcta» en `completitud.py`. |
| **HU47-T1** Marcador del espacio en blanco | A medias (cuenta entera, 1 h) | `respuesta_esperada` ya existe y se valida; falta fijar el marcador `___` en el fragmento y documentarlo. |

Las demás tareas empiezan de cero. No existe nada de racha, cinturón, Dojo, partida ni notificaciones en `user-api`, `mobile` ni en los modelos. La app Android (`mobile/`) tiene hoy inicio de sesión, «Mis cursos» y la pantalla de inicio, y ninguna pantalla de ejercicio.

Dependencias de otros sprints, todas hechas: HU13-T1 (modelo de problema), HU22-T1 (user-api como resource server), HU22-T4 (inicio de sesión en la app), HU24-T1 (APP_ENVIO) y HU26-T2 (unirse a un curso).

## 2. La idea del roadmap en cinco líneas

1. **Es el último sprint: lo que no entra el 27 no se hace.** No hay un Sprint 5 donde mover trabajo, así que recortar significa dejar algo fuera del MVP y decirlo en la entrega. Hay 117 h por hacer y 54 h de capacidad (6 días hábiles, del viernes 20 al viernes 27, × 3 h × 3 personas). Ni siquiera con 4 h al día entra todo: el 27 se cierran 61,5 h de 117. Hay que fijar el alcance final hoy (D12).
2. **Todo el sprint cuelga de Juan Diego, y de tres piezas suyas.** H1 (la ruta interna que entrega la respuesta esperada y las alternativas) abre la corrección móvil; HU53-T1 y HU53-T2 abren el Dojo (15 y 14 piezas detrás); HU57-T1 abre los duelos (18 detrás). Las dos primeras son lo primero de su lista; la de los duelos solo se hace si sobra tiempo.
3. **Carlos tiene 0 h en el Sprint 4 y 7 h de arrastre.** Es frontend web y el sprint es móvil y backend. Hay trabajo para él: el formulario del docente (H3), el diseño de los cinturones, dos documentos de decisión, el cierre del repositorio (C3) y, si acepta, dos tareas de Django y cuatro pantallas simples de Compose (sección 7). Con eso pasa de 7 h a 22 h.
4. **Nueve niveles de alcance, de lo que más vale a lo que menos.** Arrastre del Sprint 3, corrección móvil, Dojo y cierre del proyecto son el mínimo (52 h); después van el resumen de la sesión, la racha, los cinturones a la vista, el trabajo sin señal y reanudar, los avisos push y, al final, los duelos. Cada nivel se puede mostrar solo; los duelos dependen de casi todo lo anterior y son lo primero que se saca.
5. **Lo que falta en el plan está en las costuras entre servicios y en el cierre.** Nueve huecos (H1 a H9, sección 5) suman 15 h que ninguna tarea cuenta, y como es el último sprint hay cuatro piezas de cierre (C1 a C4, 6 h): datos de demo del celular, recorrido de aceptación, repositorio listo para entregar, y revisión de SonarQube con rotación de secretos.

Reglas que no cambian: se trabaja en la rama personal, solo se lleva a `main` lo que ya funciona allí, por Pull Request con otro revisor; commit `carpeta: qué hace (HU# T#)`; control de entendimiento antes del commit.

---

## 3. Las olas: el orden global, mezclando a los tres

Una **ola** agrupa las tareas que pueden empezar cuando terminaron las de las que dependen (las ya hechas cuentan como terminadas). Se lee de arriba hacia abajo; dentro de una celda van por bloque y de la que más desbloquea a la que menos. Una tarea solo espera a sus dependencias, no a que termine toda su ola. Las abreviaturas **H1 a H9** son los huecos y **C1 a C4** las piezas de cierre de la sección 5.

| Ola | Juan Diego | Carlos | Gael |
|---|---|---|---|
| **Ola 0<br>Decisiones** | **D12** alcance final y horas<br>**D13** de dónde sale el tema de un problema<br>**D14** dónde se guarda un intento móvil | — | — |
| **Ola 1** | **H1** Ruta interna con la respuesta esperada y las alternativas, y su cliente en user-api · 1,5 h<br>**HU53-T1** Migración Flyway de SesionDojo · 1 h<br>**HU50-T1** Modelo APP_CINTURON (lenguaje, nivel y puntos) y su migración · 1 h<br>**C4** Revisar SonarQube y rotar los secretos expuestos · 1 h<br>**HU49-T1** Migración Flyway de APP_RACHA · 1 h<br>**HU57-T1** Modelo Partida con sus jugadores y su migración · 2 h<br>*Total: 7,5 h* | **HU33-T5** Probar el layout del editor en 1366 px y en 1920 px · 1 h<br>**HU35-T5** Prueba: al reabrir el editor aparece lo último que se escribió · 1 h<br>**HU36-T6** Prueba a mano del flujo completo: escribir, enviar y ver el resultado · 1 h<br>**HU63-T3** Mostrar al docente el total de puntos de la evaluación · 1 h<br>**HU47-T1** Marcador ___ del espacio en blanco y dónde se guarda la respuesta esperada · 1 h<br>**HU54-T1** Documentar la regla de dificultad (dos aciertos suben, dos errores bajan) · 1 h<br>**C3** Dejar el repositorio listo: README, guía de demo, lo que quedó fuera y etiquetas de Git · 1,5 h<br>**H4** Campo `tema` en Problema, que la IA lo proponga y que viaje a user-api · 2 h<br>**HU59-T1** Elegir el mecanismo de tiempo real y registrar la decisión · 1 h<br>*Total: 10,5 h* | **HU40-T5** Prueba del error de sintaxis (falta esa de las pruebas de errores) · 1 h<br>**HU63-T4** Pruebas de la suma de puntos y de que no haya puntos negativos · 1 h<br>**HU64-T4** Pruebas del cierre del intento por tiempo · 1 h<br>**H5** Proyecto de Firebase, `google-services.json` y credencial del servidor · 1 h<br>*Total: 4 h* |
| **Ola 2** | **HU45-T1** Corregir «predecir la salida» comparando el texto normalizado · 2 h<br>**HU46-T2** Entregar las alternativas mezcladas y sin indicar la correcta · 2 h<br>**HU47-T2** Corregir «completar el espacio» ignorando espacios y mayúsculas · 2 h<br>**HU53-T2** Entregar el siguiente ejercicio de la sesión sin repetir · 2 h<br>**HU50-T2** Sumar puntos al cinturón del lenguaje al resolver un ejercicio · 1 h<br>**H6** La app envía la zona horaria y user-api la guarda · 1 h<br>**HU51-T1** Calcular los puntos que faltan para el siguiente cinturón · 1 h<br>**HU58-T1** Guardar la secuencia de ejercicios al crear la partida · 2 h<br>**HU57-T2** Crear la partida e invitar al rival · 2 h<br>**HU60-T1** Consulta del historial de partidas del estudiante · 1 h<br>*Total: 16 h* | **H3** Formulario del docente: alternativas, salida esperada y espacio en blanco · 3 h<br>**HU50-T3** Diseñar la imagen de cada cinturón, del blanco al negro · 2 h<br>*Total: 5 h* | **HU47-T3** Pantalla con el campo de texto dentro del fragmento de código · 1 h<br>**HU52-T1** Configurar Firebase Cloud Messaging en la app y guardar el token del dispositivo · 2 h<br>*Total: 3 h* |
| **Ola 3** | **H2** Ruta donde el celular envía su respuesta; corregir la opción múltiple · 2 h<br>**HU54-T2** Elegir el siguiente ejercicio según esa regla · 2 h<br>**HU56-T3** Sumar los puntos de la sesión al cinturón del lenguaje · 0,5 h<br>**HU56-T1** Calcular los temas en los que más falló durante la sesión · 1,5 h<br>**HU49-T2** Contar días seguidos con la hora local del estudiante · 2 h<br>**HU50-T4** Puntos por tipo de ejercicio y prueba de la subida de nivel · 1 h<br>**HU58-T2** Entregar el mismo ejercicio a ambos jugadores en cada turno · 2 h<br>**H7** Aceptar o rechazar el reto y listar las invitaciones pendientes · 1 h<br>**HU57-T4** Caducidad del reto a las 24 horas (configurable) · 1 h<br>*Total: 13 h* | **HU51-T2** Barra de avance hacia el siguiente cinturón · 2 h<br>*Total: 2 h* | **HU45-T2** Pantalla «predecir la salida»: fragmento de código y campo de respuesta · 2 h<br>**HU45-T4** Pruebas: correcta, incorrecta y correcta con espacios de más · 1 h<br>**HU46-T3** Pantalla de opción múltiple con botones de al menos 48 dp · 2 h<br>**HU46-T4** Prueba: la alternativa correcta no viaja al celular antes de responder · 1 h<br>**HU47-T4** Prueba con respuestas escritas distinto pero válidas · 1 h<br>**HU48-T1** Verificar que ningún ejercicio móvil llame al sandbox ni a la IA · 1 h<br>**HU53-T3** Pantalla del Dojo con contador de aciertos · 2 h<br>**HU53-T4** Prueba: en una sesión no se repite ningún ejercicio · 1 h<br>**HU51-T4** Probar el cálculo en todos los niveles · 0,5 h<br>**HU52-T3** Desactivar los avisos y elegir la hora · 1 h<br>**HU57-T3** Pantalla para retar a un compañero del curso · 2 h<br>*Total: 14,5 h* |
| **Ola 4** | **HU54-T3** Guardar el nivel de dificultad alcanzado al terminar · 1 h<br>**C1** Datos de demo del celular: intentos móviles, racha y cinturón de ejemplo · 1,5 h<br>**HU49-T4** Qué actividad mantiene la racha del día, con su prueba · 1 h<br>**HU52-T2** Tarea diaria que avisa a quien está por perder su racha · 2 h<br>**HU58-T3** Impedir que un jugador se adelante en la secuencia · 1 h<br>**HU59-T2** Actualizar el marcador de la partida en el servidor · 2 h<br>*Total: 8,5 h* | **HU56-T2** Pantalla de resumen: aciertos y temas a repasar · 2 h<br>**HU49-T3** Mostrar la racha en la pantalla principal · 2 h<br>**HU51-T3** Mensaje especial para quien ya tiene el cinturón negro · 0,5 h<br>*Total: 4,5 h* | **HU48-T2** Mostrar al instante si acertó y, si falló, la respuesta correcta · 1 h<br>**HU45-T3** Límite de intentos por ejercicio y los que le quedan; cada intento queda en APP_ENVIO · 1 h<br>**HU54-T5** Prueba con rachas de aciertos y de errores · 1 h<br>**HU56-T4** Pruebas: una sesión perfecta y una sin aciertos · 1 h<br>**HU55-T1** Guardar el estado de la sesión al salir de la pantalla · 1 h<br>**H8** Pantalla de juego: aceptar el reto y responder la secuencia · 2 h<br>*Total: 7 h* |
| **Ola 5** | **HU54-T4** Nivel de dificultad inicial de un estudiante nuevo · 1 h<br>**H9** Cerrar la partida: puntaje, ganador y empate · 1,5 h<br>**HU59-T4** Desconexión de un jugador: 30 segundos para volver · 1 h<br>*Total: 3,5 h* | — | **C2** Recorrido de aceptación en un celular con los datos de la demo · 2 h<br>**HU48-T3** Guardar la respuesta sin señal y reenviarla al volver la conexión · 2 h<br>**HU55-T2** Reanudar una sesión interrumpida: ofrecer continuar donde quedó · 1,5 h<br>**HU55-T3** Botón «Terminar sesión» con confirmación · 0,5 h<br>**HU52-T4** Prueba: el aviso llega a un celular real · 1 h<br>**HU59-T3** Marcador en vivo en la pantalla de la partida · 2 h<br>**HU58-T4** Prueba: una partida completa con dos cuentas · 1 h<br>*Total: 10 h* |
| **Ola 6** | **HU60-T2** Ranking del curso por partidas ganadas, con desempate · 2 h<br>*Total: 2 h* | — | **HU48-T4** Prueba en modo avión: la respuesta se envía una sola vez · 1 h<br>**HU55-T4** Prueba: cerrar la app a mitad de sesión y volver a abrirla · 1 h<br>**HU59-T5** Prueba con conexión lenta · 1 h<br>*Total: 3 h* |
| **Ola 7** | — | — | **HU60-T3** Pantallas de historial y de ranking · 2 h<br>**HU60-T4** Prueba: empates, estudiantes sin partidas y no inscrito (403) · 1 h<br>*Total: 3 h* |

---

## 4. Orden personal de cada integrante

El orden sale de la simulación de la sección 7 (4 h por día desde hoy, que es lo que se recomienda): es el orden real en que cada uno termina sus tareas, no el de las olas, así nadie queda esperando una tarea bloqueada mientras tiene otra lista. «Necesita de otros» muestra solo lo que depende de otra persona (entre paréntesis, de quién); «—» significa que puedes seguir cuando termines la fila anterior. «Libera» es cuántas piezas se desbloquean al terminarla. «Termina» es el día en que termina con 4 h diarias. Una \* marca una dependencia que agrega este roadmap. La línea **— corte —** marca hasta dónde llega el viernes 27; la última columna de «Qué es» dice a qué nivel de alcance pertenece cada tarea.

### Juan Diego · 50,5 h · 35 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera | Termina |
|---|---|---|---|---|---|---|
| 1 | **H1** | Ruta interna con la respuesta esperada y las alternativas, y su cliente en user-api *(hueco)* · *Corrección móvil* | 1,5 h | — | 16 | vie 20 nov |
| 2 | **HU45-T1** | Corregir «predecir la salida» comparando el texto normalizado · *Corrección móvil* | 2 h | — | 10 | vie 20 nov |
| 3 | **HU46-T2** | Entregar las alternativas mezcladas y sin indicar la correcta · *Corrección móvil* | 2 h | — | 3 | lun 23 nov |
| 4 | **H2** | Ruta donde el celular envía su respuesta; corregir la opción múltiple *(hueco)* · *Corrección móvil* | 2 h | — | 6 | lun 23 nov |
| 5 | **HU53-T1** | Migración Flyway de SesionDojo · *Dojo* | 1 h | — | 15 | mar 24 nov |
| 6 | **HU47-T2** | Corregir «completar el espacio» ignorando espacios y mayúsculas · *Corrección móvil* | 2 h | HU47-T1 (Carlos) | 2 | mar 24 nov |
| 7 | **HU50-T1** | Modelo APP_CINTURON (lenguaje, nivel y puntos) y su migración · *Dojo* | 1 h | — | 11 | mar 24 nov |
| 8 | **HU53-T2** | Entregar el siguiente ejercicio de la sesión sin repetir · *Dojo* | 2 h | — | 14 | mié 25 nov |
| 9 | **HU50-T2** | Sumar puntos al cinturón del lenguaje al resolver un ejercicio · *Dojo* | 1 h | — | 5 | mié 25 nov |
| 10 | **HU54-T2** | Elegir el siguiente ejercicio según esa regla · *Dojo* | 2 h | HU54-T1 (Carlos) | 3 | jue 26 nov |
| 11 | **HU56-T3** | Sumar los puntos de la sesión al cinturón del lenguaje · *Dojo* | 0,5 h | — | 1 | jue 26 nov |
| 12 | **HU54-T3** | Guardar el nivel de dificultad alcanzado al terminar · *Dojo* | 1 h | — | 1 | jue 26 nov |
| 13 | **HU54-T4** | Nivel de dificultad inicial de un estudiante nuevo · *Dojo* | 1 h | — | 0 | jue 26 nov |
| 14 | **C4** | Revisar SonarQube y rotar los secretos expuestos *(cierre)* · *Cierre del proyecto* | 1 h | — | 0 | jue 26 nov |
| 15 | **C1** | Datos de demo del celular: intentos móviles, racha y cinturón de ejemplo *(cierre)* · *Cierre del proyecto* | 1,5 h | — | 1 | vie 27 nov |
| 16 | **HU56-T1** | Calcular los temas en los que más falló durante la sesión · *Resumen de la sesión* | 1,5 h | H4\* (Carlos) | 2 | vie 27 nov |
| 17 | **HU49-T1** | Migración Flyway de APP_RACHA · *Racha* | 1 h | — | 8 | vie 27 nov |
| | | **— corte: hasta aquí llega el vie 27 con 4 h al día —** | | | | |
| 18 | **H6** | La app envía la zona horaria y user-api la guarda *(hueco)* · *Racha* | 1 h | — | 5 | lun 30 nov |
| 19 | **HU49-T2** | Contar días seguidos con la hora local del estudiante · *Racha* | 2 h | — | 4 | lun 30 nov |
| 20 | **HU49-T4** | Qué actividad mantiene la racha del día, con su prueba · *Racha* | 1 h | — | 0 | lun 30 nov |
| 21 | **HU51-T1** | Calcular los puntos que faltan para el siguiente cinturón · *Cinturones a la vista* | 1 h | — | 3 | mar 1 dic |
| 22 | **HU50-T4** | Puntos por tipo de ejercicio y prueba de la subida de nivel · *Cinturones a la vista* | 1 h | — | 0 | mar 1 dic |
| 23 | **HU57-T1** | Modelo Partida con sus jugadores y su migración · *Duelos* | 2 h | — | 18 | mar 1 dic |
| 24 | **HU52-T2** | Tarea diaria que avisa a quien está por perder su racha · *Avisos push* | 2 h | HU52-T1 (Gael) | 1 | mié 2 dic |
| 25 | **HU58-T1** | Guardar la secuencia de ejercicios al crear la partida · *Duelos* | 2 h | — | 12 | mié 2 dic |
| 26 | **HU57-T2** | Crear la partida e invitar al rival · *Duelos* | 2 h | — | 5 | jue 3 dic |
| 27 | **HU60-T1** | Consulta del historial de partidas del estudiante · *Duelos* | 1 h | — | 3 | jue 3 dic |
| 28 | **HU58-T2** | Entregar el mismo ejercicio a ambos jugadores en cada turno · *Duelos* | 2 h | — | 11 | vie 4 dic |
| 29 | **H7** | Aceptar o rechazar el reto y listar las invitaciones pendientes *(hueco)* · *Duelos* | 1 h | — | 2 | vie 4 dic |
| 30 | **HU57-T4** | Caducidad del reto a las 24 horas (configurable) · *Duelos* | 1 h | — | 0 | vie 4 dic |
| 31 | **HU58-T3** | Impedir que un jugador se adelante en la secuencia · *Duelos* | 1 h | — | 5 | vie 4 dic |
| 32 | **HU59-T2** | Actualizar el marcador de la partida en el servidor · *Duelos* | 2 h | HU59-T1 (Carlos) | 3 | lun 7 dic |
| 33 | **H9** | Cerrar la partida: puntaje, ganador y empate *(hueco)* · *Duelos* | 1,5 h | — | 3 | lun 7 dic |
| 34 | **HU59-T4** | Desconexión de un jugador: 30 segundos para volver · *Duelos* | 1 h | — | 0 | mar 8 dic |
| 35 | **HU60-T2** | Ranking del curso por partidas ganadas, con desempate · *Duelos* | 2 h | — | 2 | mar 8 dic |

### Carlos · 22 h · 15 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera | Termina |
|---|---|---|---|---|---|---|
| 1 | **HU33-T5** | Probar el layout del editor en 1366 px y en 1920 px *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 2 | **HU35-T5** | Prueba: al reabrir el editor aparece lo último que se escribió *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 3 | **HU36-T6** | Prueba a mano del flujo completo: escribir, enviar y ver el resultado *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 4 | **HU63-T3** | Mostrar al docente el total de puntos de la evaluación *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 5 | **HU47-T1** | Marcador ___ del espacio en blanco y dónde se guarda la respuesta esperada *(pasa de Juan Diego)* · *Corrección móvil* | 1 h | — | 5 | lun 23 nov |
| 6 | **H3** | Formulario del docente: alternativas, salida esperada y espacio en blanco *(hueco)* · *Corrección móvil* | 3 h | — | 0 | lun 23 nov |
| 7 | **HU54-T1** | Documentar la regla de dificultad (dos aciertos suben, dos errores bajan) *(pasa de Juan Diego)* · *Dojo* | 1 h | — | 4 | mar 24 nov |
| 8 | **C3** | Dejar el repositorio listo: README, guía de demo, lo que quedó fuera y etiquetas de Git *(cierre)* · *Cierre del proyecto* | 1,5 h | — | 0 | mar 24 nov |
| 9 | **H4** | Campo `tema` en Problema, que la IA lo proponga y que viaje a user-api *(hueco)* *(pasa de Juan Diego)* · *Resumen de la sesión* | 2 h | — | 3 | mié 25 nov |
| 10 | **HU50-T3** | Diseñar la imagen de cada cinturón, del blanco al negro *(pasa de Gael)* · *Cinturones a la vista* | 2 h | HU50-T1 (Juan Diego) | 0 | mié 25 nov |
| 11 | **HU59-T1** | Elegir el mecanismo de tiempo real y registrar la decisión *(pasa de Juan Diego)* · *Duelos* | 1 h | — | 4 | mié 25 nov |
| | | **— corte: hasta aquí llega el vie 27 con 4 h al día —** | | | | |
| 12 | **HU56-T2** | Pantalla de resumen: aciertos y temas a repasar *(pasa de Gael)* · *Resumen de la sesión* | 2 h | HU56-T1 (Juan Diego) | 0 | lun 30 nov |
| 13 | **HU49-T3** | Mostrar la racha en la pantalla principal *(pasa de Gael)* · *Racha* | 2 h | HU49-T2 (Juan Diego) | 0 | mar 1 dic |
| 14 | **HU51-T2** | Barra de avance hacia el siguiente cinturón *(pasa de Gael)* · *Cinturones a la vista* | 2 h | HU51-T1 (Juan Diego) | 1 | mié 2 dic |
| 15 | **HU51-T3** | Mensaje especial para quien ya tiene el cinturón negro *(pasa de Gael)* · *Cinturones a la vista* | 0,5 h | — | 0 | mié 2 dic |

### Gael · 44,5 h · 35 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera | Termina |
|---|---|---|---|---|---|---|
| 1 | **HU40-T5** | Prueba del error de sintaxis (falta esa de las pruebas de errores) *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 2 | **HU63-T4** | Pruebas de la suma de puntos y de que no haya puntos negativos *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 3 | **HU64-T4** | Pruebas del cierre del intento por tiempo *(Sprint 3)* · *Arrastre del Sprint 3* | 1 h | — | 0 | vie 20 nov |
| 4 | **H5** | Proyecto de Firebase, `google-services.json` y credencial del servidor *(hueco)* · *Avisos push* | 1 h | — | 4 | vie 20 nov |
| 5 | **HU45-T2** | Pantalla «predecir la salida»: fragmento de código y campo de respuesta · *Corrección móvil* | 2 h | HU45-T1 (Juan Diego) | 4 | lun 23 nov |
| 6 | **HU45-T4** | Pruebas: correcta, incorrecta y correcta con espacios de más · *Corrección móvil* | 1 h | HU45-T1 (Juan Diego) | 0 | lun 23 nov |
| 7 | **HU47-T3** | Pantalla con el campo de texto dentro del fragmento de código · *Corrección móvil* | 1 h | HU47-T1 (Carlos) | 0 | mar 24 nov |
| 8 | **HU46-T3** | Pantalla de opción múltiple con botones de al menos 48 dp · *Corrección móvil* | 2 h | HU46-T2 (Juan Diego) | 0 | mar 24 nov |
| 9 | **HU46-T4** | Prueba: la alternativa correcta no viaja al celular antes de responder · *Corrección móvil* | 1 h | HU46-T2 (Juan Diego) | 0 | mar 24 nov |
| 10 | **HU47-T4** | Prueba con respuestas escritas distinto pero válidas · *Corrección móvil* | 1 h | HU47-T2 (Juan Diego) | 0 | mié 25 nov |
| 11 | **HU48-T1** | Verificar que ningún ejercicio móvil llame al sandbox ni a la IA · *Corrección móvil* | 1 h | HU45-T1 (Juan Diego), HU46-T2 (Juan Diego), HU47-T2 (Juan Diego) | 0 | mié 25 nov |
| 12 | **HU48-T2** | Mostrar al instante si acertó y, si falló, la respuesta correcta · *Corrección móvil* | 1 h | H2\* (Juan Diego) | 3 | mié 25 nov |
| 13 | **HU45-T3** | Límite de intentos por ejercicio y los que le quedan; cada intento queda en APP_ENVIO · *Corrección móvil* | 1 h | HU45-T1 (Juan Diego), H2\* (Juan Diego) | 0 | mié 25 nov |
| 14 | **HU53-T3** | Pantalla del Dojo con contador de aciertos · *Dojo* | 2 h | HU53-T2 (Juan Diego) | 4 | jue 26 nov |
| 15 | **HU53-T4** | Prueba: en una sesión no se repite ningún ejercicio · *Dojo* | 1 h | HU53-T2 (Juan Diego) | 0 | jue 26 nov |
| 16 | **HU55-T1** | Guardar el estado de la sesión al salir de la pantalla · *Sin señal y reanudar* | 1 h | — | 3 | jue 26 nov |
| 17 | **HU54-T5** | Prueba con rachas de aciertos y de errores · *Dojo* | 1 h | HU54-T2 (Juan Diego) | 0 | vie 27 nov |
| 18 | **HU48-T3** | Guardar la respuesta sin señal y reenviarla al volver la conexión · *Sin señal y reanudar* | 2 h | — | 1 | vie 27 nov |
| | | **— corte: hasta aquí llega el vie 27 con 4 h al día —** | | | | |
| 19 | **HU55-T2** | Reanudar una sesión interrumpida: ofrecer continuar donde quedó · *Sin señal y reanudar* | 1,5 h | — | 1 | lun 30 nov |
| 20 | **C2** | Recorrido de aceptación en un celular con los datos de la demo *(cierre)* · *Cierre del proyecto* | 2 h | C1 (Juan Diego) | 0 | lun 30 nov |
| 21 | **HU56-T4** | Pruebas: una sesión perfecta y una sin aciertos · *Resumen de la sesión* | 1 h | HU56-T1 (Juan Diego), HU56-T3 (Juan Diego) | 0 | lun 30 nov |
| 22 | **HU55-T3** | Botón «Terminar sesión» con confirmación · *Sin señal y reanudar* | 0,5 h | — | 0 | lun 30 nov |
| 23 | **HU48-T4** | Prueba en modo avión: la respuesta se envía una sola vez · *Sin señal y reanudar* | 1 h | — | 0 | mar 1 dic |
| 24 | **HU55-T4** | Prueba: cerrar la app a mitad de sesión y volver a abrirla · *Sin señal y reanudar* | 1 h | — | 0 | mar 1 dic |
| 25 | **HU52-T1** | Configurar Firebase Cloud Messaging en la app y guardar el token del dispositivo · *Avisos push* | 2 h | HU49-T1 (Juan Diego) | 3 | mar 1 dic |
| 26 | **HU51-T4** | Probar el cálculo en todos los niveles · *Cinturones a la vista* | 0,5 h | HU51-T1 (Juan Diego) | 0 | mié 2 dic |
| 27 | **HU52-T3** | Desactivar los avisos y elegir la hora · *Avisos push* | 1 h | — | 0 | mié 2 dic |
| 28 | **HU52-T4** | Prueba: el aviso llega a un celular real · *Avisos push* | 1 h | HU52-T2 (Juan Diego) | 0 | jue 3 dic |
| 29 | **HU57-T3** | Pantalla para retar a un compañero del curso · *Duelos* | 2 h | HU57-T2 (Juan Diego) | 0 | vie 4 dic |
| 30 | **H8** | Pantalla de juego: aceptar el reto y responder la secuencia *(hueco)* · *Duelos* | 2 h | H7 (Juan Diego), HU58-T2 (Juan Diego) | 1 | lun 7 dic |
| 31 | **HU58-T4** | Prueba: una partida completa con dos cuentas · *Duelos* | 1 h | HU58-T3 (Juan Diego) | 0 | lun 7 dic |
| 32 | **HU59-T3** | Marcador en vivo en la pantalla de la partida · *Duelos* | 2 h | HU59-T2 (Juan Diego) | 1 | mar 8 dic |
| 33 | **HU59-T5** | Prueba con conexión lenta · *Duelos* | 1 h | — | 0 | mar 8 dic |
| 34 | **HU60-T3** | Pantallas de historial y de ranking · *Duelos* | 2 h | HU60-T1 (Juan Diego), HU60-T2 (Juan Diego) | 0 | mié 9 dic |
| 35 | **HU60-T4** | Prueba: empates, estudiantes sin partidas y no inscrito (403) · *Duelos* | 1 h | HU60-T2 (Juan Diego) | 0 | mié 9 dic |

---

## 5. Huecos: lo que ninguna tarea cubre

Se encontraron mirando el código de `main` contra las tareas del Sprint 4. Cada uno es una pieza que otra tarea necesita y que nadie tiene asignada. Las horas son estimaciones y ya están en la simulación como piezas propias.

| Hueco | Dónde | Qué falta | Horas | Quién | Lo necesitan |
|---|---|---|---|---|---|
| **H1** | admin-api + user-api | Ruta interna de admin-api que entrega la respuesta esperada y las alternativas (con cuál es la correcta) y su cliente en user-api. Hoy la ruta interna no las incluye. | 1,5 h | Juan Diego | H2, HU45-T1, HU46-T2, HU47-T2 |
| **H2** | user-api | Ruta única donde el celular envía su respuesta y elige el corrector según el tipo; incluye corregir la opción múltiple, que ninguna tarea corrige. | 2 h | Juan Diego | C1, HU45-T3, HU48-T2 |
| **H3** | admin-web | Formulario del docente: hoy alternativas, salida esperada y espacio en blanco son un aviso de «pendiente». Construir los tres campos. | 3 h | Carlos | — |
| **H4** | admin-api + ai-service | Campo `tema` en Problema (modelo, migración, serializador, la IA lo propone) y en la ruta interna. HU56 lo necesita y no existe. | 2 h | Carlos | HU56-T1 |
| **H5** | mobile + user-api | Crear el proyecto de Firebase, bajar `google-services.json` y dejar la credencial del servidor en variables de entorno. | 1 h | Gael | HU52-T1 |
| **H6** | mobile + user-api | La app envía la zona horaria del estudiante y user-api la guarda; sin eso la racha no puede usar su hora local. | 1 h | Juan Diego | HU49-T2 |
| **H7** | user-api | Aceptar o rechazar el reto y listar las invitaciones pendientes: nadie lo hace y la partida no arranca sin eso. | 1 h | Juan Diego | H8 |
| **H8** | mobile | Pantalla de juego: aceptar el reto y responder la secuencia turno a turno, reutilizando las pantallas de ejercicio. | 2 h | Gael | HU58-T4 |
| **H9** | user-api | Cerrar la partida: puntaje por respuesta, quién gana y qué pasa en un empate. El ranking (HU60) cuenta partidas ganadas. | 1,5 h | Juan Diego | HU60-T2 |

### Piezas de cierre del proyecto

Al ser el último sprint hay que entregar algo que se pueda mostrar y entender. Ninguna está en `historias-tareas.md`.

| Pieza | Dónde | Qué es | Horas | Quién | Necesita |
|---|---|---|---|---|---|
| **C1** | infra | Ampliar `datos_demo.py`: sembrar intentos móviles, una racha y un cinturón de ejemplo para que la demo del celular tenga datos. | 1,5 h | Juan Diego | H2, HU50-T2 |
| **C2** | mobile | Recorrido de aceptación en un celular con los datos de la demo: recorrer los criterios CA-45.x en adelante y anotar lo que falle. | 2 h | Gael (con Juan Diego) | C1, HU48-T2 |
| **C3** | docs | Dejar el repositorio listo para entregar: README, guía de demo con lo nuevo, lista de lo que quedó fuera del MVP y etiquetas de Git de los cuatro sprints. | 1,5 h | Carlos (con Gael) | — |
| **C4** | infra | Revisar los resultados de SonarQube y corregir lo grave; rotar los secretos que quedaron expuestos (contraseña de Gmail, clave de Gemini, Judge0, clave interna). | 1 h | Juan Diego | — |

Dos observaciones más que no suman horas:

- **Las pantallas de ejercicio se reutilizan.** HU45-T2, HU46-T3 y HU47-T3 son las tres pantallas de un ejercicio; el Dojo (HU53-T3) y la partida (H8) deben mostrar ejercicios con esas mismas piezas. Conviene que Gael las deje como un componente que recibe un ejercicio, para no repetirlas.
- **Cómo llega el estudiante a un ejercicio.** La app no tiene lista de ejercicios del curso y ninguna tarea la construye. En este plan el camino es el Dojo (y, después, la partida); no hace falta lista, pero HU45 a HU47 solo se prueban desde ahí o con datos sembrados hasta que exista HU53-T3.
- **Variables nuevas.** H5 agrega la credencial de Firebase y H1 reutiliza `INTERNAL_API_KEY`: ninguna debe subir al repositorio; deben ir a `.env.example` sin valor.

---

## 6. Decisiones abiertas y riesgos

### Decisiones que conviene cerrar el primer día

| # | Decisión | Recomendación | Afecta |
|---|---|---|---|
| **D12** | Alcance final y horas. Es el último sprint: no hay un Sprint 5 al que pasar lo que no entre. | **Alcance mínimo de 52 h** (niveles 0 a 3 de la sección 7: arrastre, corrección móvil, Dojo y cierre) con **4 h al día**: con eso la corrección móvil y el Dojo quedan terminados el viernes 27 y el cierre llega al lunes 30 por el recorrido de aceptación. Todo lo demás (resumen de la sesión, racha, cinturones a la vista, trabajo sin señal, reanudar, avisos y duelos) sale del MVP y se anota en la entrega como no entregado. Se agregan niveles en ese orden solo si se adelanta. Si en cambio se mantienen 3 h al día, el viernes 27 solo entran 42,5 de las 52 h. Decide el Product Owner; si la fecha del 27 puede moverse, el plan completo (sin recortes) termina el 9 de diciembre con 4 h al día. | Todo el sprint |
| **D13** | De dónde sale el «tema» de un problema (decisión abierta nº 2 del registro de cambios) | Campo `tema` en Problema (texto corto, opcional), que la IA propone al generar y el docente corrige al revisar; vacío se muestra como «General». Es la opción más barata (H4, 2 h). Solo importa si el resumen de la sesión (nivel 4) entra en el alcance; si no, H4 se descarta. | H4, HU56-T1, HU56-T2 |
| **D14** | Dónde se guarda un intento móvil (decisión abierta nº 3 del registro de cambios) | Cada intento móvil es una fila de `APP_ENVIO` con `plataforma = movil`, sin sandbox ni IA (HU24-T1 y HU45-T3 ya lo suponen). Se propone guardar la respuesta de texto en `codigo`, el resultado en `resultado` y un puntaje de 100 si es correcta y 0 si no. Evita una tabla nueva y mantiene el avance por cuenta (HU24). | H2, HU45-T3, HU48-T2, HU24 |

Las dos decisiones que ya son tareas: **HU54-T1** (regla de dificultad del Dojo, ya acordada: dos aciertos suben un nivel, dos errores lo bajan) y **HU59-T1** (mecanismo de tiempo real, solo si los duelos entran). Para HU59-T1 se recomienda consulta periódica cada 2 s: es lo más barato de construir y de probar con conexión lenta.

### Riesgos

- **Último sprint y 111 h contra 54 h.** Es la decisión abierta nº 4 del registro de cambios («Sprint 4: Juan Diego 45 h, Carlos 0 h y Gael 45 h»), que sigue sin rebalancearse en `historias-tareas.md`. Sin D12 hoy, lo que no entre se pierde sin que nadie lo haya decidido.
- **Juan Diego es el cuello.** Con el reparto completo tiene 50,5 h para 24 h de capacidad hasta el 27 (con 4 h al día). Dentro del alcance mínimo tiene 21,5 h, que caben en las 24 h de capacidad con poco margen. Cada hora que se le quita a su cadena adelanta a Gael, porque casi todas las tareas móviles esperan una de backend.
- **La entrega de una tarea a otra persona tarda un día.** Las Pull Requests necesitan otro revisor y la simulación cuenta una tarea ajena como lista al día siguiente. Si Juan Diego termina HU53-T2 el jueves 26, la pantalla del Dojo de Gael llega el viernes y el recorrido de aceptación (C2), el lunes. Para recuperar ese día hay que revisar las PR el mismo día y avisar al terminar.
- **Gael tiene todo lo móvil y las pruebas.** 44,5 h en el plan completo, y la app Android no tiene hoy ninguna pantalla de ejercicio. Si H1 y H2 tardan, sus pantallas se prueban con respuestas simuladas; eso se debe acordar antes (un `FakeApi` como el de `MisCursos`).
- **Sin recorrido de aceptación no hay MVP.** Es lo que pide el hito y es lo último que se hace; si se atrasa, C2 se acorta (solo corrección móvil y Dojo) antes que dejarlo sin hacer.
- **Firebase necesita una cuenta y un celular real** (solo si los avisos entran). HU52-T4 se prueba en un teléfono físico, no en el emulador.
- **El tiempo real es el módulo con más riesgo** (solo si los duelos entran). HU59 (marcador en vivo, desconexión, conexión lenta) es lo único del sprint sin antecedentes en el repositorio.
- **Los pendientes del Sprint 3 siguen abiertos.** HU33-T5, HU35-T5 y HU36-T6 son pruebas a mano de la web; si se dejan, el MVP web cierra sin recorrido de aceptación. Se pasan a Carlos, que ya conoce esas pantallas.
- **Secretos expuestos.** La contraseña de Gmail, la clave de Gemini, Judge0 y la clave interna aparecieron en archivos y registros del proyecto; hay que rotarlos antes de entregar (C4).

---

## 7. Reparto y ritmo

Del viernes 20 al viernes 27 hay 6 días hábiles: 18 h por persona con 3 h diarias y 24 h con 4 h diarias. La simulación respeta las dependencias y el orden de la sección 4 (una tarea de otra persona cuenta como lista al día siguiente de terminada) y usa el reparto de la tabla de más abajo (tramos 1 y 2).

Como es el último sprint, la pregunta no es qué se mueve al siguiente sino **hasta qué nivel se llega**. La tabla suma los niveles en orden de valor, de arriba hacia abajo:

| Nivel | Se suma | Horas acumuladas | Con 4 h al día: entra el vie 27 · termina | Con 3 h al día: entra el vie 27 · termina |
|---|---|---|---|---|
| 0 | Arrastre del Sprint 3 | 7 h | 7 h · vie 20 nov | 7 h · lun 23 nov |
| 1 | Corrección móvil | 31,5 h | 31,5 h · mié 25 nov | 31,5 h · vie 27 nov |
| 2 | Dojo | 46 h | 46 h · vie 27 nov | 41 h · lun 30 nov |
| 3 | Cierre del proyecto **(alcance mínimo)** | 52 h | 50 h · lun 30 nov | 42,5 h · mié 2 dic |
| 4 | Resumen de la sesión | 58,5 h | 53,5 h · lun 30 nov | 44,5 h · mié 2 dic |
| 5 | Racha | 65,5 h | 54,5 h · mar 1 dic | 44,5 h · jue 3 dic |
| 6 | Cinturones a la vista | 72,5 h | 56,5 h · mié 2 dic | 46,5 h · vie 4 dic |
| 7 | Sin señal y reanudar | 79,5 h | 59,5 h · mié 2 dic | 46,5 h · vie 4 dic |
| 8 | Avisos push | 86,5 h | 60,5 h · jue 3 dic | 47,5 h · mié 9 dic |
| 9 | Duelos | 117 h | 61,5 h · mié 9 dic | 48,5 h · mar 15 dic |

Cómo leerlo:

- **Alcance mínimo (niveles 0 a 3, 52 h).** Con 4 h al día entran 50 h el viernes 27 y todo termina el lun 30 nov; con 3 h, 42,5 h y termina el mié 2 dic. Es el único corte que se acerca a la fecha.
- **Corrección móvil y Dojo (niveles 0 a 2, 46 h)** quedan completos el viernes 27 con 4 h al día. Lo que pasa al lunes es el cierre del proyecto, sobre todo el recorrido de aceptación (C2), que necesita lo anterior terminado.
- **Cada nivel más cuesta una o dos jornadas.** Con 4 h al día, sumar el resumen de la sesión lleva el final al lun 30 nov; sumar la racha, al mar 1 dic; los cinturones a la vista, al mié 2 dic; y todo, incluidos los duelos, al mié 9 dic.
- **3 h al día no alcanzan** ni para el alcance mínimo en la fecha. La alternativa es mover la fecha del cierre; el plan completo con 3 h termina el 15 de diciembre.
- **El reparto con Carlos importa menos que las horas.** Pasar tareas a Carlos adelanta a Juan Diego y a Gael unas pocas horas, pero la fecha la marca la cadena de Juan Diego (H1, HU53-T1, HU53-T2) y la entrega de un día a otro, no el reparto.

**Reparto sugerido.** Sale de buscar qué se puede pasar sin que nadie aprenda una tecnología nueva (tramo 1) y qué sí lo exige (tramo 2). Si se acepta, hay que cambiar la columna «Resp.» en `docs/historias-tareas.md`.

| Tramo | Tarea | Horas | De → a | Por qué se puede pasar |
|---|---|---|---|---|
| 1 | **HU54-T1** Documentar la regla de dificultad (dos aciertos suben, dos errores bajan) | 1 h | Juan Diego → Carlos | Es un documento con la regla ya acordada (dos aciertos suben, dos errores bajan); Juan Diego lo aprueba. |
| 1 | **HU59-T1** Elegir el mecanismo de tiempo real y registrar la decisión | 1 h | Juan Diego → Carlos | Es una decisión escrita (WebSocket, SSE o consulta periódica); Carlos la redacta con la recomendación de la sección 6 y Juan Diego la aprueba. |
| 1 | **HU50-T3** Diseñar la imagen de cada cinturón, del blanco al negro | 2 h | Gael → Carlos | Es diseño gráfico (la imagen de cada cinturón), no código; Carlos entrega las imágenes y Gael las pone en la app. |
| 2 | **H4** Campo `tema` en Problema, que la IA lo proponga y que viaje a user-api | 2 h | Juan Diego → Carlos | Es un campo en un modelo de Django (modelo, migración, serializador); lo tiene hecho `codigo_base` (HU34-T1) como ejemplo. |
| 2 | **HU47-T1** Marcador ___ del espacio en blanco y dónde se guarda la respuesta esperada | 1 h | Juan Diego → Carlos | Es definir el marcador `___` y documentarlo en admin-api; el modelo ya tiene `respuesta_esperada`. |
| 2 | **HU49-T3** Mostrar la racha en la pantalla principal | 2 h | Gael → Carlos | Pantalla simple de Compose; Gael deja el patrón en `InicioScreen` y Carlos lo sigue. |
| 2 | **HU51-T2** Barra de avance hacia el siguiente cinturón | 2 h | Gael → Carlos | Barra de progreso en Compose; sin lógica propia. |
| 2 | **HU51-T3** Mensaje especial para quien ya tiene el cinturón negro | 0,5 h | Gael → Carlos | Un mensaje en lugar de la barra; sale de HU51-T2. |
| 2 | **HU56-T2** Pantalla de resumen: aciertos y temas a repasar | 2 h | Gael → Carlos | Pantalla de resumen en Compose; sin lógica propia. |

Con ambos tramos quedan Juan Diego con 35 piezas y 50,5 h, Carlos con 15 y 22 h, y Gael con 35 y 44,5 h.

### Qué queda fuera si se aplica D12

Es lo que se anota como «no entregado en el MVP», en este orden de sacar (de abajo hacia arriba en la tabla de niveles). Es decisión del Product Owner:

| Orden de salida | Nivel | Horas | Qué queda sin cubrir |
|---|---|---|---|
| 1 | Duelos | 30,5 h | HU57 a HU60: retos entre compañeros, marcador en vivo, historial y ranking. |
| 2 | Avisos push | 7 h | HU52: avisos de racha y hora configurable. |
| 3 | Sin señal y reanudar | 7 h | HU48-T3 y T4 (responder sin señal) y HU55 (reanudar una sesión del Dojo): la respuesta no se guarda sin conexión y la sesión no se retoma. |
| 4 | Cinturones a la vista | 7 h | HU50-T3 y T4 y HU51: el estudiante gana puntos de cinturón pero no ve la imagen ni cuánto le falta para el siguiente. |
| 5 | Racha | 7 h | HU49: la racha no existe. |
| 6 | Resumen de la sesión | 6,5 h | HU56-T1, T2 y T4: no hay pantalla de resumen al terminar una sesión (y H4, el campo `tema`, se descarta). |

Si se acepta, hay que anotar en `docs/historias-tareas.md` y en el README (C3) qué historias quedaron sin cubrir, y cuáles criterios de aceptación (CA-49.x a CA-60.x, entre otros) no se cumplen, para que la entrega diga la verdad.

---

## 8. Gestión

- Decidir D12 a D14 hoy y registrarlas como ADR-9 en `docs/decisiones-tecnicas-adr.md`, con las recomendaciones de la sección 6. D12 es la que no se puede dejar: sin ella el sprint cierra con lo que alcance a quedar hecho, no con lo que se eligió.
- Crear en Trello las tarjetas del alcance elegido con los responsables de la sección 4 y pasar el resto a una lista «Fuera del MVP» (no a un sprint siguiente: no existe). Marcar HU46-T1 como hecha y HU47-T1 como a medias; las 7 del Sprint 3 se reabren con lo que falta.
- Si se acepta el reparto, cambiar la columna «Resp.» en `docs/historias-tareas.md` (tramos 1 y 2 de la sección 7) y anotar H1 a H9 y C1 a C4 en `docs/registro-cambios.md`.
- Poner las etiquetas de Git de los sprints 1 a 4, que el ADR-6 pide y siguen sin existir (`git tag -l` está vacío): una por sprint sobre el último commit de cada uno, y `git push origin --tags`.
- Antes de entregar, recorrer en el celular los criterios del alcance elegido con los datos de la demo (C2, Gael): el hito dice que cierra el MVP, y la entrega debe decir qué criterios no se cumplen.
- Hacer el recorrido de la demo con el profesor siguiendo `guia-demo-profesor.md` y los datos de `datos_demo.py`, ya ampliados por C1; esa guía hoy no menciona nada móvil.

## 9. Cómo mantener esto al día

La página interactiva del roadmap (la misma que la del Sprint 3: una pestaña por persona, se marca cada tarea al terminarla y avisa cuál sigue) lleva el mismo orden que la sección 4 y la línea de corte del viernes 27 con 4 h al día. Si cambia el reparto o se decide D12, hay que regenerar las dos. Las horas de los huecos son estimaciones: al terminar H1, H2 y H4 se corrigen y se vuelve a correr la simulación.

**Cómo se comprobó.** Las 66 tareas se leyeron de `docs/historias-tareas.md` y se confrontaron con 90 h (45 h de Juan Diego, 45 h de Gael, 0 h de Carlos); todas las dependencias externas existen y están hechas; el grafo no tiene ciclos; el estado de HU46-T1 y HU47-T1 se verificó en `admin-api/problemas/`; y los huecos se verificaron buscando cada pieza en `admin-api`, `user-api`, `admin-web` y `mobile`.
