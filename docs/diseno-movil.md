# Diseño de la app móvil

La app móvil copia el estilo de **Consoly**: fondo casi negro azulado, tarjetas con borde fino, botones azules grandes con el texto en mayúsculas, etiquetas en letra monoespaciada y una barra de navegación inferior con el botón de Batallas al centro. **Solo hay tema oscuro** (la app ignora el tema del teléfono). Este documento explica cómo está armado para que una pantalla nueva se vea igual sin copiar estilos.

## Qué se debe compilar primero

El rediseño se escribió sin poder compilar Compose (solo se pueden correr las pruebas JVM). Antes de nada:

```powershell
cd mobile
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Si Gradle marca un error en un archivo de `ui/`, casi siempre es un import o el nombre de un ícono. Los íconos salen de `material-icons-extended`; si un nombre no existe en la versión del BOM, se cambia por uno parecido (`Icons.Rounded.<Nombre>`).

## Dependencias

- `androidx.compose.material:material-icons-extended` (versión del BOM de Compose): los íconos de las pantallas.
- Fuentes propias en `app/src/main/res/font/`: **Inter** (regular, medium, semibold, bold) para el texto y **JetBrains Mono** (regular, bold) para código, etiquetas y cifras. Ambas son OFL; sus licencias están en `mobile/licencias/`.

## Paleta (tema oscuro fijo)

| Token | Valor | Uso |
|---|---|---|
| `lienzo` | `#0B0F17` | Fondo de toda pantalla |
| `fondo` | `#151B27` | Tarjetas, chips, píldoras |
| `fondoSuave` | `#1B2333` | Cajas dentro de una tarjeta |
| `borde` / `bordeFuerte` | `#263044` / `#34405A` | Bordes finos |
| `texto` / `textoSuave` | `#F2F5FA` / `#8B95A7` | Texto |
| `acento` | `#2B90E8` | Texto azul, bordes de chips |
| `primario` | `#0078D4` | Botón principal |
| `exito` / `error` | `#4CC15F` / `#F85149` | Resultado correcto / incorrecto |
| `Dorado` | `#F5A524` | Tarjeta de puntos |
| `NaranjaDeBatalla` | ver `Color.kt` | Anillo del botón de Batallas |

Las constantes del código (`CodigoNormal`, `CodigoPalabra`, `CodigoTexto`, `CodigoNumero`, `CodigoComentario`, `CodigoLlamada`, `CodigoSigno`, `CodigoNumeroDeLinea`) pintan el código resaltado.

## Tema (`ui/theme/`)

- `Color.kt`: `ColoresMC` (versión oscura usada siempre), constantes de cabecera, dorado, banner, código y `paletaDe(lenguaje)`.
- `Type.kt`: Inter para texto; `labelMedium` y `labelSmall` en JetBrains Mono con letra espaciada (se escriben con `.uppercase()` en el sitio); `labelLarge` en JetBrains Mono negrita para botones; `EstiloDeCodigo` en mono.
- `Theme.kt`: `MasterCodeTheme` siempre usa el esquema oscuro y publica `MC.colores`. Esquinas de 8/10/12/16/20 dp.

## Componentes (`ui/componentes/`)

| Archivo | Qué hay |
|---|---|
| `Pantalla.kt` | `HeroMC` (cabecera plana con línea abajo), `CabeceraMC`, `BotonDeIconoSobreHero`, `BotonDeTextoSobreHero`, `PantallaMC(cabecera, pie, contenido)` |
| `Consoly.kt` | `EtiquetaMC`, `PildoraMC`, `TemporizadorMC`, `ChipMC`, `IntentosMC`, `DestinoMC`, `BarraInferiorMC`, `TarjetaDorada`, `BannerMC` |
| `FormatoDeTiempo.kt` | `formatoDeTiempo(segundos)` → «00:16» (con prueba JVM) |
| `Botones.kt` | `BotonPrincipal` (14 dp, mayúsculas, 56 dp de alto), `BotonSecundario` (borde y texto azules), `BotonDeTexto` |
| `Tarjetas.kt` | `TarjetaMC`, `InsigniaMC`, `AvatarMC`, `EstadoVacio`, `MensajeDeError` |
| `Progreso.kt` | `BarraDeProgreso` (acepta `colorDelRelleno`), `AnilloDeProgreso` |
| `Portada.kt` | `PortadaDeCurso`, `FichaDeLenguaje`, `LogoLenguaje` |
| `Esqueletos.kt`, `Pestanas.kt`, `Campos.kt`, `Marca.kt` | Carga, píldoras que se deslizan, campos de texto, logo |

Código de ejercicios (`ui/ejercicios/`): `TokenizadorDeCodigo.kt` parte una línea en trozos por tipo (pura, con pruebas) y `CodigoResaltado.kt` los pinta (`PanelDeCodigo` con números de línea y botones de zoom, `BloqueDeCodigo`, `TextoDeCodigo`).

Todo objetivo táctil mide 44 dp o más, y los íconos decorativos llevan `contentDescription = null` mientras que los que informan llevan texto.

## Navegación

La barra inferior (`BarraInferiorMC`) tiene cinco destinos (`DestinoMC`): **Diario**, **Dojo**, **Batallas** (botón central), **Cursos** y **Perfil**. `MainActivity` guarda la pestaña elegida y la pasa a cada pantalla como `pie`. Cada vez que se entra a Dojo, Batallas o Perfil se usa un ViewModel nuevo (datos frescos y sesión de Dojo limpia). Fuera de las pestañas, sin barra: los ejercicios del Dojo, el resumen de la sesión, la partida y los avisos de práctica (que se abren desde el Diario y el Perfil). «Atrás» en cualquier pestaña lleva al Diario.

| Pestaña | Pantalla | Qué muestra |
|---|---|---|
| Diario | `ui/diario/DiarioScreen` | Racha, «Iniciar práctica», meta de hoy (0/1 según si ya practicó), puerta a Batallas |
| Dojo | `ui/dojo/DojoScreen` | Elegir lenguaje; durante la práctica la barra se oculta |
| Batallas | `ui/duelos/DuelosScreen` | Retar, invitaciones, historial y ranking |
| Cursos | `ui/cursos/MisCursosScreen` | Cursos con el logo de su lenguaje |
| Perfil | `ui/perfil/PerfilScreen` | Un cinturón por lenguaje y la puerta a los avisos |

## Pantalla de ejercicio

`MarcoDeEjercicio` muestra, de arriba abajo: el veredicto (solo al resolver: «Resuelto» o «Incorrecto», el tiempo y la respuesta correcta), la etiqueta «PRÁCTICA» con el temporizador, los chips (intentos restantes y tipo), el título, el código en `PanelDeCodigo` y el envío. El temporizador cuenta desde que se abre el ejercicio y se detiene al resolverlo.

## Animaciones y detalles

Todas están en `ui/componentes/Animaciones.kt` (más dos retoques en `Botones.kt` y `Consoly.kt`) y solo ocurren al aparecer o al tocar; ninguna bloquea el toque.

| Qué | Dónde se ve |
|---|---|
| `Modifier.entrada(retrasoMs)`: la pieza sube y se aclara; con retrasos de 70 ms se escalonan | Diario, lista de cursos, tarjetas de cinturón, resumen del Dojo y cada ejercicio nuevo |
| `Modifier.aparecerConRebote()`: crece con un pequeño rebote | Veredicto «Resuelto» / «Incorrecto» |
| `contadorAnimado(valor)`: el número sube desde 0 | Aciertos, puntos sumados y total del resumen |
| `ConfetiMC`: confeti que cae una vez | Resumen con aciertos y ejercicio acertado |
| `Crossfade` entre pestañas (220 ms) | `MainActivity` |
| Pestaña activa: color y escala animados; vibración suave al cambiar | `BarraInferiorMC` |
| `BotonPrincipal` se achica al pulsar | Todos los botones principales |
| Llama de la racha que late (solo con racha) | Tarjeta de racha del Diario |
| Vibración corta al corregirse la respuesta | `MarcoDeEjercicio` |

Regla: las listas animan solo las primeras 6 tarjetas, para que al desplazarse no vuelvan a animarse. Cada ejercicio del Dojo se envuelve en `key(problemaId)`, así el tiempo, el desplazamiento y la entrada empiezan de cero con cada ejercicio.

## Ambiente de programación

La app se ve como un editor de código abierto:

- **Fondo de editor.** `fondoAurora()` es quieto: el lienzo con dos luces muy suaves (azul y violeta) y una grilla de puntos tenue. Se probó código pasando por detrás y se quitó: en movimiento detrás del texto marea y se lee mal.
- **Etiquetas como comentarios.** `EtiquetaMC` escribe `// META DE HOY`: las barras en el gris de comentario y el texto en mayúsculas mono. El lector de pantalla no lee las barras. `comentario = false` las quita (el lenguaje de cada curso).
- **Tarjetas como ventanas.** `VentanaMC(archivo = "racha.py")` es una `TarjetaMC` con barra de ventana: tres puntos (rojo, amarillo, verde) y el nombre del archivo. Se usa en el Diario (`racha.py`, `meta_de_hoy.json`, `batalla.sh`) y en los temas del resumen (`temas_para_repasar.log`). El panel de código de los ejercicios lleva los mismos tres puntos.
- **Prompt de terminal.** `TextoQueSeEscribe(prefijo = "> ")` pone el prompt en azul antes del texto que se escribe: `> Práctica de hoy▌` en el Diario y `$ Entrena programación…` en el login.
- **Botones en letra de código.** `labelLarge` usa JetBrains Mono, así que todos los botones se leen como código.

## Efectos continuos (`ui/componentes/Efectos.kt`)

Lo que hace que la app «se mueva sola». Cada efecto lee su valor animado solo al dibujar, así que repinta sin recomponer la pantalla. Con «Quitar animaciones» (Accesibilidad) o la escala de animación en 0 (Opciones de desarrollador), `animacionesActivas()` devuelve `false` y todo queda quieto.

| Efecto | Qué hace | Dónde |
|---|---|---|
| `Modifier.fondoAurora()` | Fondo quieto: dos luces suaves y una grilla de puntos (no se anima a propósito) | `PantallaMC(conAurora = true)`: Diario, inicio de sesión y resumen del Dojo |
| `Modifier.resplandor(color, radio)` | Halo de luz que respira cada 1,7 s, dibujado por fuera de la pieza | Botón destacado, botón de Batallas, llama de la racha, logo del login, anillo de aciertos |
| `Modifier.brilloQueCruza(forma)` | Destello diagonal que cruza la pieza y descansa | `BotonPrincipal(destacado = true)` y `TarjetaDorada` |
| `Modifier.anilloGiratorio(colores)` | Anillo con degradado circular que gira | Botón de Batallas y llama de la racha |
| `Modifier.flotar()` | Sube y baja unos píxeles, despacio | Logo del login |
| `TextoQueSeEscribe` | Texto que se escribe letra por letra con cursor azul parpadeando; el lector de pantalla lee el texto completo | Título del Diario y lema del login |

`HeroMC(transparente = true)` quita el fondo y la línea de la cabecera para que se vea la aurora. `BotonPrincipal(destacado = true)` se usa solo para la acción estrella de cada pantalla («Iniciar práctica», «Ingresar»).

## Resumen de la sesión

Anillo de aciertos (con contadores animados y confeti), `TarjetaDorada` con el «DESGLOSE DE PUNTOS» (aciertos, puntos sumados y el total del cinturón), tarjeta del cinturón, temas a repasar y `BannerMC` con el mensaje.

## Lo que Consoly tiene y esta app no

No se inventaron datos que no existen: no hay retos diarios, puntos por ejercicio, dificultad, lenguaje del ejercicio, ruta de lecciones (falta la pantalla del curso, HU29) ni botones de ajuste de líneas. El Diario usa solo la racha y si ya practicó hoy.

## Bordes del sistema (edge-to-edge)

`MainActivity` llama a `enableEdgeToEdge(...)` con barras oscuras y dibuja bajo la barra de estado y la de navegación. Los insets se manejan **en un solo lugar** para no sumarlos dos veces:

- `HeroMC` se aparta de la barra de estado y de los lados.
- `PantallaMC` aparta el cuerpo de los lados y, si no hay `pie`, de la barra de navegación y del teclado. Con `pie`, es el pie quien se aparta de la barra del sistema.
- Las pantallas de ejercicio (`MarcoDeEjercicio`) no usan `safeDrawingPadding`: su padre se encarga.

Si una pantalla nueva no usa `PantallaMC`, debe aplicar `windowInsetsPadding(WindowInsets.safeDrawing)` una sola vez.

## Imágenes

Las ilustraciones de la app salen de `docs/img/` (generadas con los prompts de `docs/prompts-imagenes.md`), ya recortadas y en WebP dentro de `app/src/main/res/drawable-nodpi/`. Se usan así:

| Imagen | Dónde se ve |
|---|---|
| `icono_fondo` + `icono_primer_plano` | Ícono de la app (adaptativo, también monocromo) |
| `ilu_logo_claro` | `LogoMC`, en el login |
| `portada_python / java / javascript / otro` | `PortadaDeCurso` (ya no se usa en «Cursos», que lleva el logo del lenguaje) |
| `img_cinturon_*` | Tarjeta de cinturón del perfil (una por nivel) |
| `ilu_racha` | Tarjeta de racha del Diario (en gris y tenue si la racha es 0) |
| `ilu_vacio_cursos / invitaciones / historial / ranking`, `ilu_mascota` | `EstadoVacio(ilustracion = …)` |
| `ilu_duelo_victoria / derrota / empate` | Resultado de la partida |
| `ilu_duelos_banner` | Cabecera de Duelos |
| `ilu_sin_conexion` | Error de la partida |

`ilu_exito` e `ilu_sesion_terminada` quedan disponibles en `res/drawable-nodpi/` para el resumen de sesión y la sesión vencida; todavía no se usan en ninguna pantalla. Los vectores viejos (`ic_launcher_*.xml`, `cinturon_*.xml`) ya no se usan y se pueden borrar cuando se confirme el cambio.

`EstadoVacio` acepta un `icono` (círculo con ícono) o una `ilustracion` (imagen de 150 dp); si se pasan las dos, gana la ilustración.

## Para probar a mano

1. Recorrer las cinco pestañas y volver con «atrás»: siempre se llega al Diario.
2. Empezar una práctica: la barra desaparece durante los ejercicios y vuelve en la elección de lenguaje.
3. Resolver un ejercicio bien y otro mal: se ven el veredicto, el tiempo y la respuesta correcta.
4. Subir el tamaño de letra del sistema al máximo: nada debe cortarse ni quedar fuera de la pantalla.
5. Girar el teléfono: la cabecera, el cuerpo y la barra respetan las muescas laterales.
6. Activar «Quitar animaciones» en Accesibilidad: los anillos y los brillos quedan quietos y los textos aparecen completos.
7. Con TalkBack: las pestañas se anuncian como pestañas, el temporizador dice el tiempo y los intentos dicen cuántos quedan.
