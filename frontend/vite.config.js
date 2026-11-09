import { fileURLToPath } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
// vitest/config's defineConfig is Vite's, plus the `test` section used by Vitest.
import { configDefaults, defineConfig } from 'vitest/config'

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
    // e2e/ holds the Playwright tests (npm run e2e); they need the real backend and are not run here.
    exclude: [...configDefaults.exclude, 'e2e/**'],
    css: true,
    // routes.jsx imports './lazyPages'; in component tests that becomes src/test/eagerPages.js (the same pages,
    // loaded directly), so a slow first page load cannot run past a test's wait. See that file.
    alias: [
      { find: /^\.\/lazyPages$/, replacement: fileURLToPath(new URL('./src/test/eagerPages.js', import.meta.url)) },
    ],
    // Generous limits so the same tests pass on slower machines (e.g. Windows with antivirus scanning),
    // where setting up the simulated browser can take 10-15x longer than on a fast one.
    testTimeout: 20_000,
    hookTimeout: 20_000,
  },
})
