/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Adresa backend-ului Spring Boot (implicit http://localhost:8080). */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
