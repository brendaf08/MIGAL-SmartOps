import type { AxeMatchers } from 'vitest-axe';

/** vitest-axe declara sus matchers en el espacio `Vi`; con Vitest 5 se extienden en el módulo `vitest`. */
declare module 'vitest' {
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type, @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars
  interface Assertion<T = any> extends AxeMatchers {}
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface AsymmetricMatchersContaining extends AxeMatchers {}
}
