# Diseño de la app móvil

La app móvil usa el mismo lenguaje visual que la web (`user-web/src/diseno.css`), inspirado en GitHub: una cabecera oscura en cada pantalla, un lienzo gris claro u oscuro según el teléfono, tarjetas con borde fino, el azul de los enlaces como acento y un botón principal verde. Este documento explica cómo está armado para que una pantalla nueva se vea igual sin copiar estilos.

## Qué se debe compilar primero

El rediseño se escribió sin poder compilar Compose (solo se verificó la parte sin Compose: 352 pruebas JVM en verde). Antes de nada:

```powershell
cd mobile
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Si Gradle marca un error en un archivo de `ui/`, casi siempre es un import o el nombre de un ícono. Los íconos salen de `material-icons-extended`; si un nombre no existe en la versión del BOM, se cambia por uno parecido (`Icons.Rounded.<Nombre>`).

## Dependencias nuevas

- `androidx.compose.material:material-icons-extended` (versión del BOM de Compose): los íconos de las pantallas.
- Fuentes propias en `app/src/main/res/font/`: **Inter** (regular, medium, semibold, bold) para el texto y **JetBrains Mono** (regular, bold) para código, marcadores y cifras. Ambas son OFL; sus licencias están en `mobile/licencias/`.

## Estilo GitHub (web y móvil)

Las dos webs (`user-web`, `admin-web`) y la app comparten los mismos tonos, tomados de GitHub (Primer):

| Token | Claro | Oscuro |
|---|---|---|
| Lienzo / tarjeta | `#F6F8FA` / `#FFFFFF` | `#0D1117` / `#151B23` |
| Texto / texto suave | `#1F2328` / `#59636E` | `#F0F6FC` / `#9198A1` |
| Borde | `#D1D9E0` | `#3D444D` |
| Acento (enlaces) | `#0969DA` | `#4493F8` |
| Botón principal (verde) | `#1F883D` | `#238636` |
| Barra superior | `#25292E` | `#151B23` |

- Esquinas de 6 dp en botones y campos, 8 en tarjetas y 12 en hojas; casi sin sombras (se separa con bordes).
- Botón principal verde (`primario` en `ColoresMC`, `--primario` en `diseno.css`); el resto de botones son neutros con borde.
- Los enlaces de texto son azules y se subrayan solo al pasar el cursor.
- **Sin adornos de «IA»:** nada de degradados, brillos, vidrio, texto con degradado ni botones en píldora. El encabezado de cada pantalla es una caja plana con borde fino (web) o una barra de un solo color con una línea abajo (móvil). Los cursos son cajas con el nombre en azul y un punto con el color del lenguaje, como los repositorios fijados de GitHub; ya no llevan portada. Los estados vacíos de la web usan un ícono gris en lugar de ilustraciones. Las imágenes de `docs/img/` quedan para el ícono de la app, el login y los estados vacíos/resultados del móvil.
- **Inicio de sesión y registro (web):** pantalla dividida como el registro de GitHub. A la izquierda un panel oscuro con la marca, el lema, los beneficios y la mascota; a la derecha el formulario con etiquetas arriba, asterisco rojo en lo obligatorio, ayuda bajo la contraseña y el botón verde a todo el ancho. En pantallas angostas el panel se reduce a una franja con el lema. Componente: `Acceso` en `components/Pantalla.*` (acepta `beneficios`, `enlace` y `ilustracion`).
- **Inicio de sesión (móvil):** cabecera oscura con la marca y una tarjeta con borde fino para el formulario.
- `diseno.css` es el mismo archivo en `user-web` y `admin-web`: si cambias uno, cambia el otro.

## Tema (`ui/theme/`)

- `Color.kt`: `ColoresMC` con los tokens de la web (lienzo, fondo, fondoSuave, texto, textoSuave, borde, bordeFuerte, acento, acentoSuave, primario, exito, error). Hay una versión clara y otra oscura, y se elige sola según el teléfono. También están las constantes del «hero» (`HeroA/B/C`, `HeroBrillo`), las del texto sobre él (`SobreHero`, `SobreHeroSuave`), el podio (`Oro`, `Plata`, `Bronce`) y `paletaDe(lenguaje)` con el degradado de cada lenguaje (Python azul→cian, Java naranja→carmesí, JavaScript amarillo→verde azulado, otros violeta).
- `Type.kt`: `Tipografia` con Inter y `EstiloDeCodigo` con JetBrains Mono.
- `Theme.kt`: `MasterCodeTheme(darkTheme, content)` arma el `ColorScheme` de Material 3 a partir de los tokens y publica `MC.colores` para leerlos en cualquier composable. Ya no se usa el color dinámico de Android: la marca debe verse igual en todos los teléfonos.

Regla: **verde y rojo solo como texto o trazo, nunca como relleno** (igual que en la web). Para un resultado se usa una insignia neutra con el texto en verde o rojo.

## Componentes (`ui/componentes/`)

| Archivo | Qué hay |
|---|---|
| `Pantalla.kt` | `HeroMC`, `CabeceraMC` (título, sobretítulo, volver, acciones), `BotonDeIconoSobreHero`, `BotonDeTextoSobreHero`, `PantallaMC` (cabecera + cuerpo con insets) |
| `Botones.kt` | `BotonPrincipal`, `BotonSecundario`, `BotonDeTexto` (con modo `compacto` de 44 dp) |
| `Tarjetas.kt` | `TarjetaMC`, `InsigniaMC`, `AvatarMC`, `EstadoVacio`, `MensajeDeError` |
| `Progreso.kt` | `BarraDeProgreso`, `AnilloDeProgreso` |
| `Portada.kt` | `PortadaDeCurso`, `FichaDeLenguaje` |
| `Esqueletos.kt` | `EsqueletoDeTarjetas` (carga con brillo) |
| `Pestanas.kt` | `PestanasMC` (píldoras que se deslizan) |
| `Campos.kt` | `CampoDeTextoMC` y `coloresDeCampo` |
| `Marca.kt` | `LogoMC` |

Todo objetivo táctil mide 44 dp o más, y los íconos decorativos llevan `contentDescription = null` mientras que los que informan llevan texto.

## Pantallas: el patrón

```kotlin
PantallaMC(
    cabecera = { CabeceraMC(titulo = "…", sobretitulo = "…", alVolver = alVolver) },
) {
    // contenido: LazyColumn, Column con scroll, etc.
}
```

Cada pantalla abre con una cabecera oscura, así que los íconos de la barra de estado son siempre claros.

## Bordes del sistema (edge-to-edge)

`MainActivity` llama a `enableEdgeToEdge(...)` y dibuja bajo la barra de estado y la de navegación. Los insets se manejan **en un solo lugar** para no sumarlos dos veces:

- `HeroMC` se aparta de la barra de estado y de los lados.
- `PantallaMC` aparta el cuerpo de los lados y de la barra de navegación (y del teclado) y avisa que el borde superior ya está cubierto.
- Las pantallas de ejercicio (`MarcoDeEjercicio`) ya no usan `safeDrawingPadding`: su padre se encarga.

Si una pantalla nueva no usa `PantallaMC`, debe aplicar `windowInsetsPadding(WindowInsets.safeDrawing)` una sola vez.

## Qué cambió en cada pantalla

Login (logo, campos con íconos y ojo para la contraseña), Mis cursos (acciones destacadas y portadas por lenguaje), Perfil, Avisos, Dojo, los tres tipos de ejercicio (código oscuro con letra mono, opciones con letra), Resumen de sesión (anillo de aciertos), Duelos (Retar, Invitaciones, Historial, Ranking con podio) y la partida (marcador en vivo en vidrio sobre la cabecera y resultado a pantalla completa). Los ViewModels, los estados y las cadenas de la lógica no cambiaron.

## Imágenes

Las ilustraciones de la app salen de `docs/img/` (generadas con los prompts de `docs/prompts-imagenes.md`), ya recortadas y en WebP dentro de `app/src/main/res/drawable-nodpi/`. Se usan así:

| Imagen | Dónde se ve |
|---|---|
| `icono_fondo` + `icono_primer_plano` | Ícono de la app (adaptativo, también monocromo) |
| `ilu_logo_claro` | `LogoMC`, en el login |
| `portada_python / java / javascript / otro` | `PortadaDeCurso` (normal, reflejada o acercada según el curso) |
| `img_cinturon_*` | Tarjeta de cinturón del perfil (una por nivel) |
| `ilu_racha` | Banner de racha (en gris y tenue si la racha es 0) |
| `ilu_vacio_cursos / invitaciones / historial / ranking`, `ilu_mascota` | `EstadoVacio(ilustracion = …)` |
| `ilu_duelo_victoria / derrota / empate` | Resultado de la partida |
| `ilu_duelos_banner` | Cabecera de Duelos |
| `ilu_sin_conexion` | Error de la partida |

`ilu_exito` e `ilu_sesion_terminada` quedan disponibles en `res/drawable-nodpi/` para el resumen de sesión y la sesión vencida; todavía no se usan en ninguna pantalla. Los vectores viejos (`ic_launcher_*.xml`, `cinturon_*.xml`) ya no se usan y se pueden borrar cuando se confirme el cambio.

`EstadoVacio` acepta un `icono` (círculo con ícono) o una `ilustracion` (imagen de 150 dp); si se pasan las dos, gana la ilustración.

## Para probar a mano

1. Cambiar el tema del teléfono (claro/oscuro) y ver que cada pantalla se lee bien.
2. Subir el tamaño de letra del sistema al máximo: nada debe cortarse ni quedar fuera de la pantalla.
3. Girar el teléfono: la cabecera y el cuerpo respetan las muescas laterales.
4. Con TalkBack: el marcador de la partida se anuncia al cambiar y los botones tienen nombre.
