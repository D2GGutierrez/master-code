# mobile

App Android de MASTER CODE (Kotlin + Jetpack Compose), paquete `com.mastercode.app`.

## Requisitos

- JDK 17 o superior.
- Android SDK (Android Studio lo instala; se recomienda usarlo).

Para abrirlo en Android Studio: **File → Open** y elige la carpeta `mobile/`.

## Comandos

Desde `mobile/` (en Windows PowerShell usa `.\gradlew.bat` en lugar de `./gradlew`):

```bash
./gradlew testDebugUnitTest   # pruebas unitarias, sin emulador
./gradlew lintDebug           # lint de Android
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/app-debug.apk
```

## Arquitectura MVVM

- **View**: funciones `@Composable`. Solo dibujan el estado y avisan los eventos al ViewModel. Sin lógica de negocio ni llamadas a la red.
- **ViewModel**: hereda de `androidx.lifecycle.ViewModel`. Expone el estado como `StateFlow` de solo lectura y recibe los eventos como funciones. No conoce `Context`, `Activity` ni composables.
- **Model**: repositorios y modelos de datos en `data/`. Se crea cuando la primera tarea lo necesite.

Paquetes:

```
com.mastercode.app/
  ui/<pantalla>/   View (<Pantalla>Screen) y ViewModel (<Pantalla>ViewModel + <Pantalla>UiState)
  ui/theme/        tema de Compose
  data/            Model (cuando exista)
```
