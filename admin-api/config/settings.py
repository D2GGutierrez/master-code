import os
from datetime import timedelta
from pathlib import Path

from cryptography.hazmat.primitives import serialization
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

# En la PC lee el .env de la raíz; en Docker las variables llegan por el entorno.
load_dotenv(BASE_DIR.parent / ".env")

SECRET_KEY = os.environ["DJANGO_SECRET_KEY"]

DEBUG = os.environ.get("DJANGO_DEBUG", "False").lower() == "true"

ALLOWED_HOSTS = os.environ.get("DJANGO_ALLOWED_HOSTS", "localhost,127.0.0.1").split(",")

# Webs que el navegador deja llamar a esta API (admin-web y user-web).
CORS_ALLOWED_ORIGINS = os.environ.get(
    "CORS_ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:3001"
).split(",")

# Único dominio de correo con el que se puede crear una cuenta.
DOMINIO_INSTITUCIONAL = os.environ["DOMINIO_INSTITUCIONAL"]

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "corsheaders",
    "rest_framework_simplejwt.token_blacklist",
    "cuentas",
    "cursos",
    "problemas",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    # Antes de CommonMiddleware: si esta responde primero (p. ej. un redirect), la respuesta sale sin cabeceras CORS.
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ["POSTGRES_DB"],
        "USER": os.environ["POSTGRES_USER"],
        "PASSWORD": os.environ["POSTGRES_PASSWORD"],
        # localhost cuando corre en la PC; dentro de Docker se usa el servicio "db".
        "HOST": os.environ.get("POSTGRES_HOST", "localhost"),
        "PORT": os.environ.get("POSTGRES_PORT", "5432"),
        # ADR-1: Django solo crea y lee tablas en admin_schema.
        "OPTIONS": {"options": "-c search_path=admin_schema"},
    }
}

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "es"
TIME_ZONE = "America/Lima"

STATIC_URL = "static/"

# Django 5.2 no lo trae por defecto (6.x sí); así las claves primarias son bigint.
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# ADR-4: la clave privada solo vive en el entorno. El .env la guarda en una línea con \n
# y la librería necesita saltos de línea reales.
jwt_clave_privada = os.environ["JWT_PRIVATE_KEY"].replace("\\n", "\n")
# La pública se calcula de la privada, así no hay que guardarla en ningún lado.
jwt_clave_publica = (
    serialization.load_pem_private_key(jwt_clave_privada.encode(), password=None)
    .public_key()
    .public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo)
    .decode()
)

SIMPLE_JWT = {
    "ALGORITHM": "RS256",
    "SIGNING_KEY": jwt_clave_privada,
    "VERIFYING_KEY": jwt_clave_publica,
    "ISSUER": "master-code-admin-api",
    "USER_ID_CLAIM": "sub",
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=int(os.environ.get("JWT_ACCESS_MINUTES", "15"))),
    # Duración web; el móvil usa JWT_REFRESH_DAYS_MOBILE (ver cuentas/tokens.py).
    "REFRESH_TOKEN_LIFETIME": timedelta(minutes=int(os.environ.get("JWT_REFRESH_MINUTES_WEB", "30"))),
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
}

JWT_REFRESH_DAYS_MOBILE = int(os.environ.get("JWT_REFRESH_DAYS_MOBILE", "30"))

# Solo la autenticación es global; cada vista declara sus propios permisos.
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": ["rest_framework_simplejwt.authentication.JWTAuthentication"],
}

EMAIL_BACKEND = os.environ.get("EMAIL_BACKEND", "django.core.mail.backends.smtp.EmailBackend")
EMAIL_HOST = os.environ.get("EMAIL_HOST", "smtp.gmail.com")
EMAIL_PORT = int(os.environ.get("EMAIL_PORT", "587"))
EMAIL_USE_TLS = os.environ.get("EMAIL_USE_TLS", "true").lower() == "true"
# Vacías por defecto para que el CI y quien no configure correo puedan arrancar.
EMAIL_HOST_USER = os.environ.get("EMAIL_HOST_USER", "")
EMAIL_HOST_PASSWORD = os.environ.get("EMAIL_HOST_PASSWORD", "")
DEFAULT_FROM_EMAIL = os.environ.get("DEFAULT_FROM_EMAIL", EMAIL_HOST_USER)

# En segundos: el enlace de recuperación de contraseña vence a los 30 minutos.
PASSWORD_RESET_TIMEOUT = 1800
# admin-web tiene la pantalla a la que lleva el enlace de recuperación.
ADMIN_WEB_URL = os.environ.get("ADMIN_WEB_URL", "http://localhost:3000")
# user-web tiene la misma pantalla para los estudiantes.
USER_WEB_URL = os.environ.get("USER_WEB_URL", "http://localhost:3001")

# Días de vigencia del código de invitación de un curso (HU26 T1).
INVITACION_DIAS = int(os.environ.get("INVITACION_DIAS", "30"))

# D3: clave compartida entre admin-api y user-api para su API interna (/internal/). Sin valor,
# la API interna rechaza todo: es preferible a dejarla abierta por olvido.
INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "")
# Dónde está user-api dentro de la red de Docker (HU28 T1).
USER_API_URL = os.environ.get("USER_API_URL", "http://localhost:8081")

# Dónde está ai-service (genera los problemas, HU17 T5). En Docker: http://ai-service:8002.
AI_SERVICE_URL = os.environ.get("AI_SERVICE_URL", "http://localhost:8002")
# Segundos que se espera su respuesta. ai-service espera hasta 90 s al LLM: aquí debe ser mayor,
# o admin-api cortaría antes que él y el docente vería «no disponible» con una respuesta en camino.
AI_SERVICE_TIMEOUT = int(os.environ.get("AI_SERVICE_TIMEOUT_SEGUNDOS", "100"))
