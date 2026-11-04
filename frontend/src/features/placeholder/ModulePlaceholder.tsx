import { PageHeader } from '../../components/layout/PageHeader'

/** Temporary page for a module that a later feature branch builds. */
export function ModulePlaceholder({ title, arrivesIn }: { title: string; arrivesIn: string }) {
  return (
    <>
      <PageHeader title={title} />
      <section className="rounded-lg border border-dashed border-brand-200 bg-white p-6 text-muted">
        This screen is built in the <strong className="text-ink">{arrivesIn}</strong> branch. Your account is allowed
        to open it.
      </section>
    </>
  )
}
