# Prompts de imágenes para Gemini (Nano Banana)

Guía para generar las imágenes del rediseño visual de user-web, admin-web y la app móvil. Cada prompt está listo para copiar y pegar; ya incluye la paleta de la marca para que todo se vea como un mismo sistema. Los prompts están en inglés porque Gemini los interpreta con más precisión; los títulos y las notas, en español.

## Cómo usarlos

1. **Una imagen por prompt**, en un chat nuevo o en el mismo. Para mantener un estilo parejo, genera primero A1 y adjúntala como imagen de referencia en las siguientes («usa el mismo estilo que esta imagen de referencia»).
2. **Relación de aspecto**: escríbela al final del prompt (por ejemplo «Aspect ratio 16:9») o elígela en el selector de Gemini. La columna de cada imagen indica cuál.
3. **Resolución**: pide la más alta disponible (2K si el modelo lo permite). Yo la reduzco para cada pantalla.
4. **Sin texto en la imagen.** Los modelos de imagen suelen escribir mal; los nombres y títulos los pongo yo encima, en código. Si Gemini dibuja letras o logos conocidos, regenera con «no text, no logos».
5. **Sin transparencia**: Gemini no devuelve fondo transparente. Los prompts marcados con fondo blanco liso se recortan después (yo me encargo, o una herramienta de «quitar fondo»).
6. **No usar logos oficiales** de Python, Java o JavaScript: tienen derechos de marca. Las portadas ya lo piden.
7. **Variantes**: si una salida queda casi bien, pide «same image but …» en lugar de empezar de cero.

## Cómo me los devuelves

Guarda cada archivo con el nombre indicado (en `Claude outputs/imagenes/` o donde prefieras) y pásamelos todos juntos. Yo los optimizo (WebP/PNG), los coloco en `user-web/public/img/`, `admin-web/public/img/` y `mobile/app/src/main/res/drawable-nodpi/`, y los conecto a las pantallas. No hace falta que los coloques tú.

**Orden recomendado si quieres empezar poco a poco:** A1, A2, A3 (la marca), luego C1 a C4 (portadas) y B1, B2 (logins). Con eso ya cambia mucho el aspecto. Lo demás se puede añadir después.

## A. Marca
### A1. Símbolo del logo
- **Archivo:** `logo-simbolo.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Logo de las tres plataformas (el texto «MASTER CODE» lo pongo yo en vector)

```text
A bold, minimal brand symbol for a coding-training platform: a rounded hexagon shield whose center contains stylized angle brackets '< >' formed by two thick rounded strokes, with a small spark at the top right corner. Violet-to-indigo gradient (#6D28D9 to #4F46E5), tiny lavender highlight on the edges. Simple enough to be readable at 32 pixels. Front view, flat, no 3D, no shadow. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Aspect ratio 1:1.
```
### A2. Símbolo para fondo oscuro
- **Archivo:** `logo-simbolo-claro.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Cabeceras oscuras (barra y hero)

```text
The same brand symbol: a rounded hexagon shield with stylized angle brackets '< >' inside and a small spark at the top right, but drawn in white and light lavender (#E0E0FF to #A78BFA) with a faint glow, designed to sit on a very dark indigo background. Flat, minimal, no shadow. Render it on a solid very dark indigo background (#0E0C22). Aspect ratio 1:1.
```
### A3. Fondo del ícono de la app
- **Archivo:** `icono-app-fondo.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Capa de fondo del ícono adaptativo de Android (1024×1024)

```text
A full-bleed square background texture for a mobile app icon: a smooth diagonal gradient from deep indigo #1B1747 (bottom left) through #3730A3 to violet #6D28D9 (top right), with a soft lavender radial glow near the top right corner and a very subtle grain. Completely empty: no shapes, no symbols, no text, no border. Aspect ratio 1:1.
```
### A4. Imagen para compartir el enlace
- **Archivo:** `og-compartir.png`
- **Relación de aspecto:** 16:9
- **Dónde se usa:** Vista previa al compartir en WhatsApp o redes (la recorto a 1200×630 y le pongo el nombre encima)

```text
A wide hero banner for a programming practice platform. The right half shows floating code editor windows made of abstract colored bars, angle brackets, a small trophy and light beams, arranged diagonally with soft depth. The left half is calm: a smooth indigo-to-violet gradient with a soft glow, intentionally empty to place a title later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 16:9.
```

## B. Acceso y fondos
### B1. Ilustración del login (web estudiante)
- **Archivo:** `login-estudiante.png`
- **Relación de aspecto:** 4:5
- **Dónde se usa:** Panel lateral oscuro del login de user-web

```text
A vertical illustration for a login screen of a coding-training app for students. A glowing laptop with a code editor window floating above it, a trophy, a flame streak icon, small stars and angle brackets orbiting around, a dark indigo night-sky atmosphere with lavender light beams. Energetic but clean and minimal. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 4:5.
```
### B2. Ilustración del login (web docente)
- **Archivo:** `login-docente.png`
- **Relación de aspecto:** 4:5
- **Dónde se usa:** Panel lateral del login de admin-web

```text
A vertical illustration for a login screen of a teacher dashboard. A friendly abstract robot assistant floating beside a large clipboard with a checklist, a magic wand leaving sparkles that turn into small exercise cards, a gradebook chart made of simple bars. Calm, trustworthy, a bit more sober than playful. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 4:5.
```
### B3. Textura del hero
- **Archivo:** `textura-hero.png`
- **Relación de aspecto:** 21:9
- **Dónde se usa:** Fondo de las cabeceras (web y móvil), la recorto según la pantalla

```text
An abstract wide background texture: smooth mesh gradient flowing from deep indigo #1B1747 on the left to violet #6D28D9 on the right, with a large soft lavender glow in the top right corner, a few very faint concentric circles and tiny sparkles. Calm, with no focal object, so text can sit on top. No shapes that look like objects. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 21:9.
```

## C. Portadas de curso (una por lenguaje)
### C1. Portada Python
- **Archivo:** `portada-python.png`
- **Relación de aspecto:** 16:9
- **Dónde se usa:** Tarjetas y hero de cursos de Python

```text
A course cover illustration with an abstract code editor window in the center-right made of colored bars, floating curly brackets, loops and small geometric snakes-like waves in the background. Color palette strictly from deep blue #1E3A8A through blue #2563EB to cyan #22D3EE, with white highlights. Do NOT draw any official Python logo or snake mascot. No text. Clean flat vector, soft gradients. Aspect ratio 16:9.
```
### C2. Portada Java
- **Archivo:** `portada-java.png`
- **Relación de aspecto:** 16:9
- **Dónde se usa:** Tarjetas y hero de cursos de Java

```text
A course cover illustration with an abstract code editor window in the center-right made of colored bars, stacked blocks and building-block shapes suggesting objects and classes, warm diagonal lines in the background. Color palette strictly from orange #F97316 through red-orange #EA580C to crimson #BE123C, with cream highlights. Do NOT draw any official Java logo or coffee cup. No text. Clean flat vector, soft gradients. Aspect ratio 16:9.
```
### C3. Portada JavaScript
- **Archivo:** `portada-javascript.png`
- **Relación de aspecto:** 16:9
- **Dónde se usa:** Tarjetas y hero de cursos de JavaScript

```text
A course cover illustration with an abstract code editor window in the center-right made of colored bars, floating browser window shapes, lightning bolts and small dots connected by lines suggesting events and the web. Color palette strictly from yellow #FACC15 through lime #A3E635 to teal #0D9488, with white highlights. Do NOT draw any official JavaScript logo. No text. Clean flat vector, soft gradients. Aspect ratio 16:9.
```
### C4. Portada genérica (otros lenguajes)
- **Archivo:** `portada-otro.png`
- **Relación de aspecto:** 16:9
- **Dónde se usa:** Cursos de cualquier otro lenguaje

```text
A course cover illustration with an abstract code editor window in the center-right made of colored bars, floating angle brackets '</>' symbols, puzzle-like blocks and a gear. Palette from deep indigo #3730A3 through violet #6D28D9 to magenta #C026D3, lavender highlights. No text, no logos. Clean flat vector, soft gradients. Aspect ratio 16:9.
```

## D. Estados vacíos, éxito y error (ilustraciones pequeñas, para web y móvil)
### D1. Sin cursos
- **Archivo:** `vacio-cursos.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Cuando el estudiante o el docente no tiene cursos

```text
A friendly empty-state illustration: an open empty bookshelf with one single book leaning, a small plant, and a floating magnifying glass with a spark, suggesting 'nothing here yet'. Light and welcoming. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D2. Sin invitaciones
- **Archivo:** `vacio-invitaciones.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Pestaña Invitaciones de Duelos, sin retos

```text
A friendly empty-state illustration: an open empty mailbox with a tiny paper plane flying away and a small crossed-swords icon floating above it, suggesting 'no challenges yet'. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D3. Sin historial
- **Archivo:** `vacio-historial.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Pestaña Historial de Duelos, sin partidas

```text
A friendly empty-state illustration: a blank scroll or logbook with a ribbon bookmark, a small hourglass and a pencil, suggesting 'no matches played yet'. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D4. Ranking vacío
- **Archivo:** `vacio-ranking.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Pestaña Ranking sin posiciones

```text
A friendly empty-state illustration: an empty three-step podium with a faint dashed outline of a trophy on top and a few sparkles, suggesting 'nobody has played yet'. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D5. Respuesta correcta / éxito
- **Archivo:** `exito.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Al enviar una solución correcta, generar problemas con IA, etc.

```text
A celebratory success illustration: a large round medal with a bold check mark on a ribbon, small stars and confetti bursts around. Joyful but clean. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D6. Victoria en duelo
- **Archivo:** `duelo-victoria.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Pantalla de resultado «Ganaste»

```text
A victory illustration: a golden trophy with raised laurel branches, radiating light beams, confetti and stars, a small crown on top. Triumphant and warm gold (#F5B301) accents combined with the indigo palette. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D7. Derrota en duelo (ánimo)
- **Archivo:** `duelo-derrota.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Pantalla de resultado «Perdiste», tono alentador

```text
An encouraging, never sad illustration: a sturdy shield with a small flame inside, a cracked-but-glowing sword resting beside it and a rising sun behind, suggesting 'learn and try again'. Warm, motivating mood. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D8. Empate en duelo
- **Archivo:** `duelo-empate.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Pantalla de resultado «Empate»

```text
A balanced illustration: two identical code windows facing each other with a glowing equals-sign shape made of light between them (made of shapes, not text), perfectly symmetrical, with small stars. Calm and fair. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D9. Sesión de práctica terminada
- **Archivo:** `sesion-terminada.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Resumen del Dojo

```text
A completion illustration: a large circular progress ring almost full with a glowing end, a small black-belt-colored ribbon, a laptop and sparkles floating, suggesting 'training session finished'. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```
### D10. Sin conexión / error
- **Archivo:** `sin-conexion.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Errores de red o del servidor

```text
A gentle error illustration: a cloud with a small unplugged cable and a tiny wrench, a few dotted lines fading out, suggesting 'connection problem, try again'. Not alarming, no red; use lavender and indigo. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 1:1.
```

## E. Duelos, racha y cinturones
### E1. Banner de duelos
- **Archivo:** `duelos-banner.png`
- **Relación de aspecto:** 16:9
- **Dónde se usa:** Cabecera de la sección Duelos

```text
A dynamic wide banner: two floating code editor windows facing each other from the left and right edges, with lightning energy meeting in the center, small trophies and sparks, a sense of friendly competition between two students. The center top is calm and empty. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 16:9.
```
### E2. Llama de racha
- **Archivo:** `racha-llama.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Banner de racha en Mis cursos

```text
A stylized flame icon, a bold teardrop-shaped flame with an inner smaller flame, gradient from warm yellow #FBBF24 through orange #F97316 to deep red-orange #EA580C, with a soft glow and two tiny sparks. Flat, simple, readable at 48 pixels. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Aspect ratio 1:1.
```
### E3. Plantilla de cinturón (repetir 7 veces)
- **Archivo:** `cinturon-<color>.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Perfil: un cinturón por nivel. Repite este prompt cambiando COLOR por: white, yellow, orange, green, blue, brown, black

```text
A martial-arts style belt tied in a neat knot, seen from the front, in COLOR, with subtle fabric texture, two stitched lines along the edges and tails hanging down in a gentle curve. Same angle, size and style for every color so the seven images look like one set. Flat vector style with soft shading. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. Keep the belt color pure COLOR; no text. Aspect ratio 1:1.
```

## F. Opcionales
### F1. Mascota de la marca (opcional)
- **Archivo:** `mascota.png`
- **Relación de aspecto:** 1:1
- **Dónde se usa:** Estados vacíos, bienvenida, onboarding

```text
A friendly mascot for a coding-training platform: a small round robot with big expressive eyes, a tiny antenna with a spark, wearing a martial-arts headband in indigo, simple limbs, smiling, in a confident pose. Flat vector with soft gradients, palette of indigo, violet and lavender with a small warm accent. Character sheet style: front view, neutral pose. Place the subject centered on a plain solid white background (#FFFFFF) with generous empty margin around it, so the background can be removed later. No text. Aspect ratio 1:1.
```
### F2. Generación con IA (docente)
- **Archivo:** `generacion-ia.png`
- **Relación de aspecto:** 4:3
- **Dónde se usa:** Pantalla donde el docente genera problemas con IA

```text
A magical creation scene: a floating wand or sparkle stream turning blank cards into exercise cards with abstract code lines and check marks, a soft glow, a small robot assistant watching. Flat modern vector illustration with soft gradients and a subtle grain, rounded geometric shapes, clean professional ed-tech look, color palette of deep indigo #1B1747, royal indigo #3730A3, violet #6D28D9 and lavender glow #A78BFA with electric indigo #4F46E5 accents. No text, no letters, no numbers, no logos, no watermark. Any code shown is only abstract colored bars, never readable text. Aspect ratio 4:3.
```

## Qué no necesito generar

- **Avatares de estudiantes**: se dibujan con la inicial del nombre, en código.
- **Favicon**: lo saco del símbolo A1.
- **Íconos pequeños** (volver, ajustes, reloj, etc.): ya son íconos vectoriales.
- **Portadas por curso concretas**: se arman con la portada del lenguaje (C1 a C4) más un motivo distinto por curso, así no hay que generar una por cada curso nuevo.
