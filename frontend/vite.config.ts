import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // La API se sirve en el mismo origen durante el desarrollo, por lo que no requiere CORS. El destino se
    // sobrescribe con VITE_API_DESTINO. Se conserva el Host original (changeOrigin: false) para que Spring reconozca
    // el mismo origen frente al encabezado Origin del navegador.
    proxy: {
      '/api': { target: process.env.VITE_API_DESTINO ?? 'http://localhost:8080', changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    css: true,
    // Las pruebas de pantalla montan Mantine completo y usan latencia simulada: con muchos archivos en paralelo
    // el límite de 5 s por prueba se supera por contención, no por un defecto.
    testTimeout: 20000,
    hookTimeout: 20000,
    maxWorkers: 6,
  },
});
