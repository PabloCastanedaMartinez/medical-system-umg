import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo, Vite (http://localhost:5173) reenvía /api al backend del equipo (cecamed-api).
// Cambia el destino con la variable CECAMED_API_URL si el backend corre en otro puerto o equipo.
const apiTarget = process.env.CECAMED_API_URL ?? 'http://localhost:8080';

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  define: {
    // `--mode mock` activa la API simulada en el navegador (ver src/mocks/mockApi.ts).
    'import.meta.env.VITE_USE_MOCK': JSON.stringify(mode === 'mock' ? 'true' : (process.env.VITE_USE_MOCK ?? 'false')),
  },
  server: {
    port: 5173,
    proxy: {
      // Conservar Host mantiene el proxy como mismo origen para Spring Security.
      '/api': { target: apiTarget, changeOrigin: false },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: false,
  },
}));
