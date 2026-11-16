# Flujo de trabajo con Git — MASTER CODE

Reglas para que el repositorio quede ordenado, cada cambio tenga un responsable y todo el equipo pueda explicar el código que entrega.

## 1. Ramas

| Rama | Para qué sirve | Quién escribe en ella |
| :--- | :--- | :--- |
| `main` | Lo que se presenta. Siempre funciona. Cada sprint se marca con una etiqueta (`sprint-1`). | Nadie directamente: solo entra lo que ya funciona en la rama personal de quien lo hizo. |
| `Gutierrez` | Trabajo diario de Juan Diego (`admin-api`, `user-api`, `ai-service`, `infra`, `docs`). | Juan Diego. |
| `carlos-junco` | Trabajo diario de Carlos (`admin-web`, `user-web`). | Carlos. |
| `gael-lajara` | Trabajo diario de Gael (`mobile` y pruebas). | Gael. |

Cada integrante tiene una rama personal permanente y trabaja en ella a diario, una tarea por commit. No se crean ramas por tarea.

## 2. Ciclo de una tarea

1. Mover la tarjeta en Trello a «En progreso».
2. Ponerse al día: en la rama personal, `git pull --rebase origin main`.
3. Hacer la tarea con un commit (formato en la sección 3), con la fecha real salvo lo indicado por el profesor.
4. Pasar el **control de entendimiento** (sección 4) y subir con `git push` a la rama personal. El CI corre en cada push a una rama personal.
5. **Solo cuando la tarea funciona en la rama personal** (CI en verde y probada a mano) se lleva a `main`: Pull Request desde la rama personal hacia `main`.
6. Otro integrante revisa. El autor no hace merge de su propio PR.
7. Merge con «Squash and merge». La rama personal no se borra: se vuelve a poner al día con `git pull --rebase origin main`. Mover la tarjeta a «Hecho».

Regla de oro: `main` nunca recibe algo que no funcione ya en la rama personal. Si un PR rompe el CI de `main`, se revierte primero y se arregla después en la rama personal.

## 3. Commits

Formato: `carpeta: qué hace (HU# T#)`

Ejemplos: `admin-api: modelo Docente y migración (HU1 T1)`, `user-api: verificación del JWT con JWKS (HU22 T1)`.

- Un commit = un cambio que se puede explicar en una línea.
- Se hacen manualmente (`git add` de archivos concretos, nunca `git add .` sin revisar `git status` y `git diff --staged`).
- Nunca se commitean `.env`, claves ni contraseñas.

## 4. Control de entendimiento (el paso anti-código-basura)

Antes del PR a `main`, quien hizo la tarea debe poder responder, sin mirar la IA:

1. ¿Qué hace cada archivo nuevo o modificado y por qué hace falta para la tarea?
2. ¿Qué pasa si borro este archivo o esta función? Si la respuesta es «nada», se borra.
3. ¿Cómo comprobé que funciona? (la prueba o el `curl` concreto)

Si no puede responder alguna, no se hace PR: se pide la explicación a la IA, se lee el código y se elimina lo que no se justifique. El revisor puede hacer esas mismas preguntas durante la revisión.

## 5. Pull Request

Título: igual que el commit. Descripción corta:

```
## Tarea
HU# T# — enlace a la tarjeta de Trello

## Qué cambia
(2 a 5 líneas)

## Cómo lo probé
(comandos o pasos)

## Control de entendimiento
[ ] Puedo explicar cada archivo nuevo
[ ] No hay código ni dependencias que la tarea no pida
[ ] Sin credenciales en el cambio
[ ] Las pruebas y el CI pasan
```

Revisor: confirma que el cambio hace solo lo que dice la tarea, que el CI está verde y que no sobra nada.

## 6. Protecciones en GitHub (Settings → Branches)

Para `main`: exigir Pull Request, exigir 1 aprobación, exigir que el CI pase y bloquear el push directo. Las ramas personales no tienen protección.

Al cerrar cada sprint se marca el estado final con una etiqueta: `git tag sprint-1` y `git push origin sprint-1`.

### Etiquetas de los cuatro sprints (C3)

Hasta el 20 de noviembre de 2026 `git tag -l` estaba vacío. Cada etiqueta va sobre el **último commit de `main` de su sprint**; el comando lo busca por fecha para no copiar hashes a mano. Se corren desde la raíz, con `main` al día (`git checkout main` y `git pull`):

```bash
git tag -a sprint-1 -m "Sprint 1: base del proyecto (5 al 16 de octubre)" "$(git rev-list -1 --before='2026-10-19 00:00' main)"
git tag -a sprint-2 -m "Sprint 2: editor, sandbox, IA e inscripción (19 al 30 de octubre)" "$(git rev-list -1 --before='2026-11-02 00:00' main)"
git tag -a sprint-3 -m "Sprint 3: calificación de punta a punta y evaluaciones (2 al 13 de noviembre)" "$(git rev-list -1 --before='2026-11-16 00:00' main)"
git tag -a sprint-4 -m "Sprint 4: app móvil y cierre del MVP (16 al 27 de noviembre)" main
git push origin sprint-1 sprint-2 sprint-3 sprint-4
```

Antes de empujar, comprueba con `git show --stat sprint-1` (y los otros) que cada etiqueta cae donde dice la tabla. Los hashes son los de hoy, para revisar que el comando acierta:

| Etiqueta | Cierre del sprint | Último commit de `main` hasta esa fecha |
| :--- | :--- | :--- |
| `sprint-1` | viernes 16 de octubre | `8bb94ac` «pruebas de inicio de sesión móvil de punta a punta (HU22 T5)» |
| `sprint-2` | viernes 30 de octubre | `3b7e4e7` «Mejoras de Interfaz Gráfica de la aplicación - Master Code» |
| `sprint-3` | viernes 13 de noviembre | `f6f5f13` «indicador de último guardado en el editor (HU35-T4)» |
| `sprint-4` | viernes 27 de noviembre | el último commit de `main` tras el Pull Request final; se etiqueta ese día |

Si un commit tiene la fecha cambiada a pedido del profesor (ver el README), `--before` usa la fecha del commit y no la real: revisa la tabla antes de empujar. Etiquetar no cambia el código de nadie, pero `git push origin` de una etiqueta es público: hazlo una sola vez.

## 7. Origen de este repositorio

Este repositorio empezó vacío al inicio del Sprint 1, para aplicar desde el primer commit las reglas de este documento. Las versiones anteriores se conservan sin cambios en los repositorios `master-code` y `master-code-tec`.

El primer commit del repositorio es el único que se hace directo en `main`; el resto entra por Pull Request desde las ramas personales. Los commits conservan su fecha real, salvo los habilitadores que el profesor pidió fechar al 2 de octubre: ahí la descripción del commit trae la fecha real.

## 8. Quién toca qué

| Integrante | Carpetas |
| :--- | :--- |
| Juan Diego Gutiérrez | `admin-api`, `user-api`, `ai-service` |
| Carlos Junco | `admin-web`, `user-web` |
| Gael La Jara | `mobile` y las tareas de pruebas |

Si una tarea exige tocar la carpeta de otra persona, se avisa en el PR y esa persona revisa.
