#!/usr/bin/env bash
# Analiza el código del monorepo con SonarQube (guía: docs/sonarqube.md). Necesita `make sonar-up` y un token en
# SONAR_TOKEN (variable de entorno o línea SONAR_TOKEN=... del .env). No imprime el token.
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$RAIZ"

# El token puede venir del entorno o del .env. Se lee solo esa línea: el .env tiene otros secretos y no se carga entero.
if [ -z "${SONAR_TOKEN:-}" ] && [ -f .env ]; then
  SONAR_TOKEN="$(grep -E '^SONAR_TOKEN=' .env | head -1 | cut -d= -f2- | tr -d '"\r' || true)"
fi
if [ -z "${SONAR_TOKEN:-}" ]; then
  echo "Falta SONAR_TOKEN. Créalo en http://localhost:9000 (Mi cuenta > Seguridad) y ponlo en el .env: SONAR_TOKEN=..." >&2
  exit 1
fi
export SONAR_TOKEN

# El servidor tarda un par de minutos en arrancar la primera vez.
if ! curl -fsS http://localhost:9000/api/system/status 2>/dev/null | grep -q '"status":"UP"'; then
  echo "SonarQube no está listo en http://localhost:9000. Ejecuta 'make sonar-up' y espera a que abra la página." >&2
  exit 1
fi

# Java se analiza sobre las clases compiladas, y las bibliotecas se copian para que Sonar entienda los tipos de Spring.
echo "Compilando user-api..."
(cd user-api && ./mvnw -B -q -DskipTests test-compile dependency:copy-dependencies -DoutputDirectory=target/dependency)

# En Git Bash de Windows, MSYS_NO_PATHCONV evita que cambie la ruta del volumen y pwd -W da la ruta que entiende Docker.
RAIZ_DOCKER="$(pwd -W 2>/dev/null || pwd)"
echo "Analizando..."
MSYS_NO_PATHCONV=1 docker run --rm \
  --network master-code-sonar \
  -e SONAR_HOST_URL=http://sonarqube:9000 \
  -e SONAR_TOKEN \
  -v "$RAIZ_DOCKER:/usr/src" \
  sonarsource/sonar-scanner-cli:latest

echo "Listo. Resultados en http://localhost:9000/dashboard?id=master-code"
