"""HU39-T3: comprueba que judge0-server no deja en su log el stdin de los envíos (las entradas de los casos ocultos).

Judge0 (Rails) registra los parámetros de cada POST /submissions, stdin incluido. La fuga se cerró desactivando el log
del contenedor en docker-compose.yml (logging: driver: none). Si el driver es none, no hay log que revisar: OK.
Si no, manda una marca inventada a Judge0 y la busca en el log (código 1 si aparece).
Uso, con el stack levantado (`docker compose up -d`): python infra/scripts/fuga_judge0_logs.py
"""

import json
import subprocess
import sys
import urllib.request
import uuid


def docker(*argumentos: str) -> str:
    return subprocess.run(["docker", *argumentos], capture_output=True, text=True, check=True).stdout.strip()


contenedor = docker("compose", "ps", "-q", "judge0-server")
if not contenedor:
    sys.exit("judge0-server no está levantado: docker compose up -d judge0-server")
driver = docker("inspect", "--format", "{{.HostConfig.LogConfig.Type}}", contenedor)
if driver == "none":
    print("OK: log de Judge0 desactivado")
    sys.exit(0)

# Marca inventada y única por corrida: si aparece en el log, Judge0 la escribió en esta corrida.
MARCA = f"MARCA-OCULTA-{uuid.uuid4().hex[:8]}"

peticion = urllib.request.Request(
    "http://127.0.0.1:2358/submissions?wait=true",
    data=json.dumps({"language_id": 71, "source_code": "print(input())", "stdin": MARCA}).encode(),
    headers={"Content-Type": "application/json"},
    method="POST",
)
urllib.request.urlopen(peticion, timeout=60).read()

veces = docker("compose", "logs", "--no-color", "judge0-server").count(MARCA)
print(f"Log de Judge0 con driver {driver}: la entrada oculta aparece {veces} veces (debe ser 0).")
sys.exit(1 if veces else 0)
