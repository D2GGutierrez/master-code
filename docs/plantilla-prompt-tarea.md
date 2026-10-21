# Plantilla de prompt para Claude Code — una tarea, un commit en la rama personal

Copia este esqueleto, rellena los campos entre `< >` con los datos de la tarea (del documento `docs/historias-tareas.md`) y pégalo en Claude Code desde la raíz del monorepo.

---

# Tarea HU<N> T<N> — <título> (Sprint <N> · MASTER CODE)

## Rol y contexto
Eres el desarrollador de `<carpeta>` en el monorepo `master-code`. Trabajas para <nombre del integrante>, estudiante que debe poder explicar cada línea que entregues. Prefiere siempre la solución más simple y legible.

## Objetivo
<qué debe existir al terminar, en 2 a 4 líneas, tomado de la descripción de la tarea>

## Dependencias
<tareas o HU que deben estar hechas; si no lo están, detente y avísame antes de escribir nada>

## Decisiones ya cerradas
<ADR y reglas que aplican: schemas, JWT, roles, etc. No las cambies>

## Alcance
SÍ: <lo que pide la tarea>
NO: <lo que pertenece a otras tareas>

## Rama
No cambies de rama ni crees ramas: trabaja en la rama personal actual (`Gutierrez`, `carlos-junco` o `gael-lajara`). No trabajes directamente en `main`.

## Pasos
1. **Explorar (solo lectura).** Resume en 5 líneas lo que ya existe y es relevante. Si algo contradice este prompt, detente y pregúntame.
2. **Implementar** lo mínimo para cumplir el objetivo, siguiendo las convenciones que ya tiene el repo.
3. **Pruebas** del comportamiento descrito en la tarea.
4. **Verificar:** lint y pruebas del CI en verde, y la comprobación manual que corresponda (`curl`, pantalla o comando).
5. **Sin commit.** Deja los cambios sin commitear. No ejecutes `git add`, `git commit` ni `git push`.

## Reglas de código mínimo (obligatorias)
- Escribe solo lo que la tarea pide. Nada de archivos, clases, funciones, opciones de configuración ni dependencias «por si acaso».
- No agregues una dependencia sin preguntarme y explicarme para qué sirve.
- Prefiere lo que ya enseña la ruta del instituto (Django MVT, Spring con Maven, Kotlin MVVM, React con componentes simples) antes que patrones avanzados.
- Nada de código muerto, comentarios obvios ni funciones que nadie llama.
- Comenta solo el **porqué** de lo que no sea evidente, con frases cortas.
- Si hay dos formas de hacerlo, elige la más corta y legible, y dime en una línea por qué.

## Restricciones
- Ninguna credencial, clave, contraseña o token completo en el repo, en logs ni en la salida.
- No toques `.env`; solo `.env.example`.
- No modifiques carpetas ajenas a la tarea.

## Criterios de terminado
<lista de casillas sacada de los criterios de aceptación de la HU>
- [ ] Solo hay cambios que la tarea justifica.
- [ ] Cambios sin commitear, en la rama personal.

## Reporte final
1. **Qué hace cada archivo creado o modificado y por qué hace falta**, en 1 o 2 líneas cada uno. Esto es lo que voy a leer antes de aceptar el cambio.
2. Qué comprobaste y con qué resultado (resumido, sin secretos).
3. **Lo que quitaste o decidiste no hacer** para mantener el código mínimo.
4. Hallazgos que deba decidir <nombre>.
5. Archivos para `git add` (uno por uno) y un mensaje de commit con el formato `carpeta: qué hace (HU<N> T<N>)`.

---

## Antes de abrir el PR (lo hace la persona, no la IA)
1. Leer el reporte final y abrir cada archivo nuevo.
2. Responder: ¿qué hace?, ¿qué pasa si lo borro?, ¿cómo lo probé? Lo que no se pueda justificar se pide eliminar a Claude Code.
3. Hacer el commit manualmente con la fecha real, subir a la rama personal y, cuando todo funciona allí, abrir el Pull Request hacia `main`.
