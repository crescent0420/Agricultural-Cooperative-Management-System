import { BackendStatus } from './features/system/BackendStatus'

/** Temporary start page for feature/frontend-setup; the router replaces it in the next branch. */
function App() {
  return (
    <>
      <header className="bg-brand-700 text-white">
        <div className="mx-auto max-w-2xl px-4 py-10">
          <p className="text-sm font-semibold tracking-wide text-white/85 uppercase">ACMS</p>
          <h1 className="mt-1 text-3xl font-semibold">Agricultural Cooperative Management System</h1>
          <p className="mt-2 text-white/90">Rwanda Cooperative Union · web frontend</p>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-8">
        <section className="rounded-lg border border-line bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Frontend setup</h2>
          <ul className="mt-3 list-disc space-y-1 pl-5 text-muted">
            <li>React and TypeScript are running.</li>
            <li>Tailwind CSS is styling this page with the ACMS colour tokens.</li>
          </ul>
          <div className="mt-4 border-t border-line pt-4">
            <BackendStatus />
          </div>
        </section>
      </main>
    </>
  )
}

export default App
