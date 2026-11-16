# SonarQube: análisis del código fuente

Antes de seguir con el Sprint 4 se analiza todo el monorepo con **SonarQube Community** corriendo en tu PC con Docker: sin cuentas, sin subir el código a ningún sitio y sin costo. Revisa bugs, vulnerabilidades, *security hotspots*, *code smells* y duplicación.

## Qué se analiza

Un solo proyecto de Sonar (`master-code`) con todas las carpetas, cada una con el analizador de su lenguaje:

| Carpeta | Lenguaje | Pruebas |
|---|---|---|
| `admin-api` (sin `migrations/`) | Python | `test_*.py` y `tests.py` |
| `ai-service/app` y `ai-service/scripts` | Python | `ai-service/tests` |
| `user-api/src/main` | Java 21 | `user-api/src/test` |
| `user-web/src` y `admin-web/src` | TypeScript, JavaScript y CSS | `*.test.ts` y `*.test.tsx` (solo user-web) |
| `mobile/app/src/main` | Kotlin | `mobile/app/src/test` |

No se analizan `node_modules`, `dist`, `build`, `target`, `.venv` ni las migraciones de Django (las genera `makemigrations`). Las pruebas se analizan aparte del código, para que sus defectos no se mezclen con los del producto.

Los archivos: `docker-compose.sonar.yml` (servidor), `sonar-project.properties` (qué analizar), `infra/scripts/sonar.sh` (el análisis) y los comandos `make sonar-up`, `make sonar` y `make sonar-down`.

## Primera vez

1. **Docker Desktop encendido.** En Windows con WSL2, SonarQube necesita que el motor de búsqueda tenga memoria virtual suficiente. Si el contenedor se cae al arrancar, ejecuta una vez en PowerShell:
   `wsl -d docker-desktop sysctl -w vm.max_map_count=524288`
   (se pierde al reiniciar WSL). Reserva unos 2 GB de RAM para Docker.
2. **Arrancar el servidor:** `make sonar-up`. La primera vez tarda un par de minutos: abre http://localhost:9000 hasta que cargue.
3. **Entrar** con usuario `admin` y clave `admin`. Te pedirá cambiarla; guarda la nueva.
4. **Crear el token:** en tu avatar > *My Account* > *Security* > *Generate Tokens*, tipo *User Token*, sin vencimiento (o el que prefieras). Cópialo ahora: no se vuelve a mostrar.
5. **Guardarlo en tu `.env`** (no en el repositorio): `SONAR_TOKEN=el_token`. La línea ya está en `.env.example`.

## Cada análisis

```
make sonar-up   # si el servidor no está encendido
make sonar
```

**En Windows sin `make`** (PowerShell no lo trae; `make` no es obligatorio): usa los comandos que hay detrás de cada uno.

```
docker compose -f docker-compose.sonar.yml up -d      # = make sonar-up
& "C:\Program Files\Git\bin\bash.exe" infra/scripts/sonar.sh      # = make sonar
docker compose -f docker-compose.sonar.yml down       # = make sonar-down
```

El script es de bash: se ejecuta con el bash de Git (ruta de arriba) o desde la terminal Git Bash con `./infra/scripts/sonar.sh`. No uses el `bash` de WSL, que no tiene Docker ni Java del lado de Windows.

`make sonar` compila `user-api` (Sonar analiza Java sobre las clases compiladas), corre el scanner en un contenedor y deja los resultados en http://localhost:9000/dashboard?id=master-code. Necesita Java 21 y Docker. Se ejecuta con Git Bash, igual que `make claves-jwt`.

Para apagar el servidor: `make sonar-down`. Los análisis se conservan en los volúmenes; para empezar de cero, `docker compose -f docker-compose.sonar.yml down -v`.

## Cómo leer el resultado

Mira en este orden, y arregla solo lo que valga la pena antes del Sprint 4:

1. **Security Hotspots y Vulnerabilities:** lo más importante. Un *hotspot* no es un fallo confirmado: hay que revisarlo y marcarlo «Seguro» o corregirlo.
2. **Bugs:** errores probables de lógica.
3. **Code Smells y Duplication:** deuda técnica. Se corrige después, por módulo.

Cada hallazgo trae la regla, una explicación y un ejemplo del arreglo. Usa el filtro por carpeta para ver un servicio a la vez.

## Lo que hay que saber

- **La cobertura aparece vacía.** Por ahora solo se hace análisis estático: no se cargan informes de cobertura (pytest-cov, JaCoCo, Vitest). El *Quality Gate* «Sonar way» exige 80 % de cobertura en el código nuevo, así que **saldrá en rojo por ese motivo**, aunque no haya bugs. Es esperado. Si la cobertura se agrega después, esa condición se evalúa de verdad.
- **Edición Community: una sola rama.** No analiza ramas ni *pull requests*; cada análisis reemplaza al anterior. Analiza siempre la misma rama (la que vayas a entregar) para que el historial sea comparable.
- **El primer análisis lo cuenta todo como «código nuevo».** Es normal. Para que el siguiente análisis solo juzgue lo que cambió, en *Project Settings* > *New Code* elige *Previous version* o una fecha de referencia (por ejemplo, el inicio del Sprint 4).
- **Falsos positivos:** se marcan en la propia interfaz (*Accept* o *False Positive*) con un comentario. No se tocan reglas globales por un caso aislado.
- **Informes de otros análisis:** el scanner deja la carpeta `.scannerwork/` en la raíz; está en `.gitignore`.
