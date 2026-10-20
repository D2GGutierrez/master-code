FROM python:3.12-slim

WORKDIR /app

# Primero solo requirements.txt: si no cambia, Docker reutiliza la capa con las dependencias.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app app

# Usuario sin privilegios: si alguien toma el proceso, no es root.
RUN useradd --system app
USER app

EXPOSE 8002

# 0.0.0.0 y no 127.0.0.1: si no, el puerto publicado no llega desde fuera del contenedor.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8002"]
