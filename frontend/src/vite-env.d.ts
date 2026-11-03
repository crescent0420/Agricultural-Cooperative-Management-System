/// <reference types="vite/client" />

// Variables read from frontend/.env files. Only names starting with VITE_ reach the browser,
// so never put a secret in them.
interface ImportMetaEnv {
  /** Origin of the Spring Boot API, e.g. https://api.example.org. Empty in development (Vite proxy). */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
