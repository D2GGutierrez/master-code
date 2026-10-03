# user-web

Web del estudiante de MASTER CODE (React + Vite, TypeScript). Usa Node 24 y npm.

Todos los comandos se ejecutan dentro de `user-web/`. Son iguales en Windows (PowerShell) y en Linux/Mac.

## Instalar

```powershell
npm install
```

## Arrancar

```powershell
npm run dev
```

Abre <http://localhost:3001>. Si el puerto 3001 está ocupado, el servidor falla en vez de usar otro.

## Revisar y construir

```powershell
npm run lint
npm run build
```

`npm run build` también revisa los tipos de TypeScript (`tsc -b`).

## Docker

```powershell
docker build -t master-code-user-web .
docker run --rm -p 3001:3001 master-code-user-web
```
