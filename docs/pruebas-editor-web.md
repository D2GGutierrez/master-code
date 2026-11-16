# Pruebas del editor de la web del estudiante (HU33-T5, HU35-T5, HU36-T6)

Las tres tareas son pruebas «a mano» del Sprint 3 que quedaron abiertas. El **20 de noviembre de 2026** se hizo una pasada automática (Chromium con Monaco real y una API simulada con estado, que se comporta como `user-api`) y aquí queda lo que se comprobó y lo que **sigue siendo a mano**: la prueba contra el sistema real, con la base y `user-api` encendidos. La casilla «A mano» la marca quien la hace, con su nombre y la fecha.

Los criterios son los de `docs/historias-tareas.md`; el resultado guardado en la base lo cubren además `EnviosApiTest`, `BorradorApiTest` y `humo.py`.

## Preparación (para las pruebas a mano)

1. `docker compose up -d` y, para tener un estudiante con un curso y un problema de escribir código, `DEMO_CLAVE=<una clave tuya> make datos-demo` (ver el README). Las cuentas y los códigos se muestran al final.
2. Abrir http://localhost:3001, iniciar sesión como estudiante, entrar a un curso y abrir un problema **de código** (el editor es `/problemas/{id}/editor`).
3. Con el motor de ejecución simulado los envíos no se califican; para ver «Correcto» hace falta el real (`docs/piston.md`).

## HU33-T5 · Layout en 1366 px y en 1920 px

**Hecho cuando:** el layout se revisa en una laptop pequeña (1366 px) y en un monitor grande (1920 px) sin desbordes.

| Comprobación | 1366×768 | 1920×1080 |
| --- | --- | --- |
| La página no se desplaza a los lados | Sin desborde | Sin desborde |
| Enunciado (izquierda) y editor (derecha) caben | 505 y 619 px | 505 y 619 px |
| Divisor al mínimo (tecla Inicio) y al máximo (tecla Fin) | Sin desborde | Sin desborde |
| Un bloque de código ancho se desplaza dentro de su panel, sin empujar la página | Sí | Sí |
| Los botones «Ejecutar» y «Enviar solución» no quedan cortados a los lados | Sí | Sí |

Capturas: `docs/img/editor-1366.png` y `docs/img/editor-1920.png`.

**Observaciones (no son desbordes):**

- En 1920 px el contenido no pasa de unos 1130 px de ancho y quedan márgenes grandes a los lados; en 1366 px ocupa casi todo. Es una decisión de diseño, no un defecto del criterio.
- En 1366×768, «Enviar solución» queda **debajo** de la parte visible del panel derecho (editor, entrada, «Ejecutar» y salida ocupan el alto): hay que bajar dentro de ese panel para llegar a él. En 1920×1080 se ve sin bajar. En ambos tamaños la página entera se desplaza además unos 72 px, por la barra de navegación.

**A mano** (con el sistema real; el navegador en 1366 y en 1920, o con las herramientas de desarrollador en esos tamaños):

- [ ] 1366 px: abrir el editor, arrastrar el divisor con el mouse de un extremo al otro y comprobar que nada se corta ni aparece barra horizontal en la página.
- [ ] 1920 px: lo mismo.
- [ ] En ambos: recargar y comprobar que el divisor queda donde se dejó (HU33-T4).
- [ ] Resultado: ______ · Quién y fecha: ______

## HU35-T5 · Al volver a abrir aparece lo último que se escribió

**Hecho cuando:** al cerrar y volver a abrir el problema aparece lo último que se escribió.

| Caso | Resultado |
| --- | --- |
| Escribir, esperar el guardado automático (≈3 s, el indicador dice «Guardado a las…»), cerrar la pestaña y abrirla de nuevo | **Pasa**: aparece lo escrito |
| Varias líneas con sangría vuelven idénticas | **Pasa** |
| Con borrador y con código base, abre el borrador (ADR-8, D8) | **Pasa** |
| Con código base y sin borrador, abre el código base | **Pasa** |
| Editor nuevo, sin borrador ni código base | **Pasa**: vacío |
| Escribir y **salir por un enlace en menos de 3 s** («Volver al problema») | **No pasa en la prueba automática**: se pierde lo escrito en esos últimos segundos |

**Hallazgo.** `useAutoguardado` guarda al ocultar la pestaña (`visibilitychange`) y al salir de la pantalla de React, pero un enlace de la web recarga toda la página: no hay desmontaje de React y, en el navegador de las pruebas, no se disparó `visibilitychange`, así que no se envió nada. El propio código lo anota como «mejor esfuerzo». Una solución chica sería escuchar también `pagehide` y mandar el guardado con `fetch(..., { keepalive: true })`; no se aplicó porque la tarea es de prueba y conviene confirmar primero con el navegador real (punto siguiente).

**A mano:**

- [ ] Escribir una línea, esperar a que diga «Guardado», cerrar la pestaña, abrir `/problemas/{id}/editor`: aparece la línea.
- [ ] Escribir una línea y, **sin esperar**, hacer clic en «Volver al problema» y volver al editor: ¿aparece o se perdió? (anotar el resultado y el navegador).
- [ ] Escribir una línea y cerrar la pestaña de inmediato; abrirla: ¿aparece o no? (anotar).
- [ ] Resultado: ______ · Quién y fecha: ______

## HU36-T6 · Escribir, enviar y ver el resultado guardado

**Hecho cuando:** flujo completo comprobado: escribir código, enviarlo y ver el resultado guardado en APP_ENVIO.

| Paso | Resultado |
| --- | --- |
| «Enviar solución» pregunta antes de enviar y dice «Este será tu intento N.º 1» | **Pasa** |
| Un código incorrecto: «Incorrecto», puntaje 0 %, casos superados 0 de 3 | **Pasa** |
| Un código correcto: intento N.º 2, «Correcto», 100 %, 3 de 3 | **Pasa** |
| El historial (`/problemas/{id}/envios`) lista los dos intentos, el más reciente primero | **Pasa** |
| El envío queda en `APP_ENVIO` (código, resultado, puntaje, número de intento) | Lo cubre `EnviosApiTest` de `user-api` (en la corrida del 20 de nov: 413 pruebas, todas pasan); **falta verlo en la base real** |

**A mano** (con el motor real, ver `docs/piston.md`):

- [ ] Escribir un programa correcto y enviarlo: aparece «Correcto», 100 %.
- [ ] Escribir uno incorrecto y enviarlo: aparece «Incorrecto» con el detalle de los casos públicos; los ocultos solo dicen si pasaron.
- [ ] Abrir el historial: están los dos intentos con su fecha, resultado y puntaje.
- [ ] En la base: `docker compose exec db psql -U $POSTGRES_USER -d $POSTGRES_DB -c "select numero_intento, resultado, puntaje, left(codigo, 30) from app_schema.app_envio order by id desc limit 5;"` muestra esas filas.
- [ ] Resultado: ______ · Quién y fecha: ______
