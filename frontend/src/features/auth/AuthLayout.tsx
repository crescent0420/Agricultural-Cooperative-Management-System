import type { ReactNode } from 'react'

/** Two panels on wide screens (brand panel + form), one column on a phone. */
export function AuthLayout({ title, intro, children }: { title: string; intro?: ReactNode; children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col lg:flex-row">
      <aside className="bg-brand-700 px-6 py-8 text-white lg:flex lg:w-[42%] lg:flex-col lg:justify-between lg:px-12 lg:py-14">
        <div>
          <p className="text-sm font-semibold tracking-wide text-white/85 uppercase">ACMS</p>
          <p className="mt-2 text-2xl font-semibold lg:text-3xl">Agricultural Cooperative Management System</p>
          <p className="mt-3 hidden max-w-md text-white/90 lg:block">
            Farmer registration, seasonal enrolment, field inspections, contributions and collection permits for the
            Rwanda Cooperative Union, in one place.
          </p>
        </div>
        <p className="mt-6 hidden text-sm text-white/80 lg:block">Rwanda Cooperative Union</p>
      </aside>

      <main className="flex flex-1 items-start justify-center px-4 py-10 lg:items-center">
        <div className="w-full max-w-md">
          <h1 className="text-2xl font-semibold text-ink">{title}</h1>
          {intro && <div className="mt-2 text-muted">{intro}</div>}
          <div className="mt-6">{children}</div>
        </div>
      </main>
    </div>
  )
}
