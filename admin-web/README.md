# admin-web

Panel web del docente de MASTER CODE (React + Vite, JavaScript). Usa Node 24 y npm.

Todos los comandos se ejecutan dentro de `admin-web/`. Son iguales en Windows (PowerShell) y en Linux/Mac.

## Instalar

```powershell
npm install
```

## Arrancar

```powershell
npm run dev
```

Abre <http://localhost:3000>. Si el puerto 3000 está ocupado, el servidor falla en vez de usar otro.

## Revisar y construir

```powershell
npm run lint
npm run build
```

## Docker

```powershell
docker build -t master-code-admin-web .
docker run --rm -p 3000:3000 master-code-admin-web
```
