/** The demo accounts created by the backend's DemoDataSeeder (URD §23), with what each one is used to prove. */
export const ACCOUNTS = {
  admin: 'demo.admin',
  registrar: 'demo.registrar',
  finance: 'demo.finance',
  officer: 'demo.officer', // Field Officer, Huye
  agronomist: 'demo.agronomist', // Huye
  officerMusanze: 'demo.officer.musanze',
  farmer: 'demo.farmer', // Aline Uwase, Huye
  farmerMusanze: 'demo.farmer.musanze', // Eric Niyonzima, Musanze
}

/** A name nobody has used yet, so the tests can run again on the same database. */
export function unique(prefix) {
  return `${prefix}-${Date.now().toString(36)}${Math.floor(Math.random() * 1296).toString(36)}`
}
