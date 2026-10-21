# Contrato de autenticación (ADR-4)

Django emite el JWT firmado con una llave privada RSA y Spring Boot solo lo verifica con la pública. `user-api` no tiene login, ni contraseñas, ni usuarios propios. Es el ADR-4 bajado a decisiones concretas, y lo primero que se construye en el Sprint 1: HU 22, HU 23 y HU 24 dependen de que funcione.

| Decisión | Qué se define | Por qué |
| :--- | :--- | :--- |
| Credenciales | Usuarios y contraseñas viven en el `auth_user` de Django (`admin_schema`), docentes y estudiantes, con un claim `rol`. | ADR-4 y ADR-1: ningún servicio escribe en el schema del otro. |
| Cómo se asigna el rol | Lo fija el endpoint de registro, nunca un campo del formulario. `registro/estudiante/` crea la cuenta activa con rol estudiante; `registro/docente/` valida el dominio y crea la cuenta inactiva, que el Product Owner activa desde el admin de Django. | Docentes y estudiantes comparten dominio: el dominio prueba que el correo es de la institución, no que su dueño sea docente. |
| Perfil del estudiante | `APP_ESTUDIANTE` en Spring es solo el perfil: se crea la primera vez que llega un token válido con ese email. | Django nunca escribe en `app_schema`. |
| Algoritmo | RS256, con `djangorestframework-simplejwt` en Django. | Con firma asimétrica `user-api` solo guarda la llave pública: aunque lo vulneren, no pueden falsificar tokens. |
| Claims | `sub`, `email`, `rol` (docente o estudiante), `institucion`, `cliente` (web o movil), `iss`, `iat`, `exp`. | El email es el identificador común entre los dos schemas; la institución la usa HU 25; `cliente` hace que el refresh conserve su duración al rotar. |
| Duración | Access de 15 min en web y móvil. Refresh de 30 min con rotación en la web y de 30 días en el móvil. | La web se usa en PC compartidas: 30 min sin actividad cierran la sesión (HU 4). El celular es personal y la app vive de volver cada día. |
| Llave pública | Django la publica sin autenticación en `GET /.well-known/jwks.json`. | Es lo que consulta `user-api` para verificar las firmas. |
| Validación en Spring | `spring-boot-starter-oauth2-resource-server`, con la URL del JWKS en la variable de entorno `JWKS_URI`; exige firma y `exp`, y valida `iss` con un validador propio. | No hay criptografía escrita a mano. |
| Rutas | Django: `POST /api/auth/login/`, `/refresh/`, `/logout/`, `/registro/estudiante/` y `/registro/docente/`. Spring: todo `/api/**` exige Bearer con rol estudiante; `/health` es público. Un token de docente no abre la API del estudiante. | |
| Llave privada | Se genera una vez, vive en `.env` fuera del repositorio y se documenta cómo regenerarla. Si se regenera, los tokens vigentes dejan de servir. | Del Done: nada de credenciales en el repositorio. |
| CORS | Django acepta los orígenes de la web del estudiante (`:3001` y `:3000`), por variable de entorno. | El navegador bloquea el login antes de que React reciba la respuesta. La app Kotlin no lo necesita. |

## Cerrar sesión

Cerrar sesión invalida el refresh al instante. El access no se puede invalidar porque un JWT no guarda estado: sigue válido hasta que vence (15 minutos como máximo).

## Cómo se retira el riesgo

Se hace de corrido: llaves RSA (HU22-T6), emisión (HU02-T1), login y renovación (HU02-T2), clave pública (HU22-T2) y verificación en Spring (HU22-T1). Está listo cuando pasan cuatro pruebas con `curl`:

1. Token válido: 200, con su email y rol.
2. Sin token: 401.
3. Firma alterada: 401.
4. Token vencido: 401.

Si algo choca con el JWKS, el plan B es que Spring lea la llave pública desde un archivo (`public-key-location`), que no depende de la red entre servicios.

## Se completa durante el Sprint 1

Estos detalles se definen en HU02-T1 y HU22-T1 y se anotan aquí: valor exacto del claim `iss`, cómo se distingue un access de un refresh, tolerancia de reloj al validar `exp` y el algoritmo de hash de contraseñas (HU04-T1: PBKDF2-SHA256, ver ADR-4). El claim `iss` es `master-code-admin-api`.
