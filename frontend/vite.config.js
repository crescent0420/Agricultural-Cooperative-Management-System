import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
// vitest/config's defineConfig is Vite's, plus the `test` section used by Vitest.
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Development only: the browser calls /api on the Vite server (same origin, so no CORS),
    // and Vite forwards the call to the Spring Boot API. See URD gap G1.
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    css: true,
    // Generous limits so the same tests pass on slower machines (e.g. Windows with antivirus scanning),
    // where setting up the simulated browser can take 10-15x longer than on a fast one.
    testTimeout: 20_000,
    hookTimeout: 20_000,
  },
})
