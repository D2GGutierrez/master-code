#!/usr/bin/env bash
# Genera la clave RSA privada con la que admin-api firma los JWT (ADR-4)
# y la guarda en JWT_PRIVATE_KEY del archivo de entorno, sin imprimirla.
set -euo pipefail

ENV_FILE="${ENV_FILE:-$(dirname "$0")/../../.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "No existe $ENV_FILE. Primero ejecuta: cp .env.example .env" >&2
  exit 1
fi

tmp=$(mktemp)
# Borra los temporales (contienen la clave) al terminar, incluso si algo falla.
trap 'rm -f "$tmp" "$tmp.env"' EXIT

openssl genrsa -quiet -out "$tmp" 2048

# PEM en una sola línea: cada salto de línea se escribe como \n (y se quita \r de Windows).
export LINEA="JWT_PRIVATE_KEY=\"$(awk '{ sub(/\r$/, ""); printf "%s\\n", $0 }' "$tmp")\""

# Reemplaza la línea si ya existe; si no, la agrega al final.
# La clave llega por ENVIRON para no pasarla como argumento visible.
awk '/^JWT_PRIVATE_KEY=/ { print ENVIRON["LINEA"]; hecho = 1; next }
     { print }
     END { if (!hecho) print ENVIRON["LINEA"] }' "$ENV_FILE" > "$tmp.env"
cat "$tmp.env" > "$ENV_FILE"

echo "Clave JWT generada y guardada en $ENV_FILE."
echo "Aviso: los tokens emitidos con la clave anterior dejan de servir; hay que volver a iniciar sesión."
