# ACMS frontend

React 19 + JavaScript + Vite web interface for the ACMS Spring Boot API.
Requirements: see the URD; design: see the Frontend Architecture document.

## Requirements

- Node.js 22.22 or newer (developed with Node 24.15, npm 11.12)
- The ACMS API running on `http://localhost:8081` (`docker compose up` or `.\mvnw.cmd spring-boot:run` in the repository root)

## Commands (run in this `frontend` folder)

| Command | What it does |
| --- | --- |
| `npm install` | Install the dependencies (once, and after `package.json` changes) |
| `npm run dev` | Start the development server on http://localhost:5173 |
| `npm run test` | Run the component tests and watch for changes (`npm run test:run` runs once) |
| `npm run lint` | Check the code with oxlint |
| `npm run build` | Type-check and build the production files into `dist/` |
| `npm run preview` | Serve the built `dist/` locally |

## How the frontend reaches the API

In development the browser calls `/api/...` on the Vite server, and Vite forwards the call to
`http://localhost:8081` (see `server.proxy` in `vite.config.js`). Browser and API therefore share one
origin and no CORS configuration is needed. For a production build, set `VITE_API_BASE_URL`
(see `.env.example`).

## Folder structure

```
src/
  api/          the only code that talks HTTP (from the authentication branch)
  auth/         session and permissions
  components/   shared UI and layout components
  features/     one folder per module
  routes/       route table and guards
  test/         test setup
  types/        the backend DTO shapes, as JSDoc comments (documentation only)
  utils/        formatting helpers
```
