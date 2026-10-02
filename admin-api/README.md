# admin-api

API de administración de MASTER CODE (Django + Django REST Framework). Usa la base PostgreSQL del `docker-compose.yml` de la raíz, solo en `admin_schema`, y lee la configuración del `.env` de la raíz.

Todos los comandos se ejecutan dentro de `admin-api/` y con la base levantada (`docker compose up -d db` desde la raíz).

## Instalar (Windows, PowerShell)

```powershell
py -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
```

En Linux/Mac: `python3 -m venv .venv && .venv/bin/python -m pip install -r requirements-dev.txt`, y en los comandos siguientes usa `.venv/bin/python`.

## Probar

```powershell
.venv\Scripts\python.exe -m ruff check
.venv\Scripts\python.exe manage.py test
```

## Migrar y arrancar

```powershell
.venv\Scripts\python.exe manage.py migrate
.venv\Scripts\python.exe manage.py runserver
```

Comprueba que responde en <http://localhost:8000/health>.

## Imagen de Docker

```powershell
docker build -t master-code-admin-api .
```
