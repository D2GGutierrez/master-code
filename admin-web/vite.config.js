import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // strictPort: si el 3000 está ocupado falla, en vez de saltar a otro puerto en silencio.
    port: 3000,
    strictPort: true,
    // En Docker, localhost es el propio contenedor: allí el destino será http://admin-api:8000 (H8).
    proxy: { '/api': process.env.API_PROXY_TARGET || 'http://localhost:8000' },
  },
})
