/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  // strictPort: si el 3001 está ocupado falla, en vez de saltar a otro puerto en silencio.
  server: { port: 3001, strictPort: true },
  // Pruebas con vitest en un DOM simulado (jsdom); Monaco se sustituye en cada prueba.
  test: { environment: 'jsdom' },
})
