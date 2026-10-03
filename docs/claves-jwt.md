# Clave RSA del JWT

## Qué es y para qué sirve

admin-api firma los tokens de sesión (JWT) con RS256 usando una clave RSA privada de 2048 bits (ADR-4).
La clave privada vive solo en la variable de entorno `JWT_PRIVATE_KEY` del `.env` local, que git ignora; nunca se sube al repositorio.
La clave pública no se guarda: se calcula a partir de la privada y se publica en `/.well-known/jwks.json`, donde user-api la consulta para verificar los tokens.

## Cómo generarla o regenerarla

Desde la raíz del repositorio, con el `.env` ya creado (`cp .env.example .env`):

```bash
make claves-jwt
```

Si no tienes `make` (por ejemplo, en Git Bash de Windows):

```bash
bash infra/scripts/claves-jwt.sh
```

El comando no imprime la clave: solo escribe o reemplaza la línea `JWT_PRIVATE_KEY` en el `.env`.

## Formato de `JWT_PRIVATE_KEY`

La clave en formato PEM, en una sola línea, entre comillas dobles, con cada salto de línea escrito como `\n`:

```
JWT_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
```

Quien la lea debe convertir `\n` en saltos de línea reales si el cargador de `.env` no lo hace solo.

## Qué pasa al regenerarla

Los tokens emitidos con la clave anterior dejan de servir: todos los usuarios tienen que volver a iniciar sesión.

## Si la clave se filtra

Regenérala de inmediato con `make claves-jwt` y reemplaza la clave en todos los entornos donde se use. Así los tokens firmados con la clave filtrada dejan de servir.
