/** Kliento sąsajos laukų tikrinimas tomis pačiomis ribomis kaip serveryje (FR-01, NFR-30). */

export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PASSWORD_PATTERN = /^(?=.*\p{L})(?=.*\d).{8,64}$/u;
export const PASSWORD_MESSAGE = 'Slaptažodis turi būti 8–64 simbolių ir turėti bent vieną raidę ir vieną skaitmenį.';

export function validateEmail(email: string): string | undefined {
  if (!email.trim()) return 'Įveskite el. paštą.';
  if (email.length > 100) return 'El. paštas negali būti ilgesnis nei 100 simbolių.';
  if (!EMAIL_PATTERN.test(email.trim())) return 'Neteisingas el. pašto formatas.';
  return undefined;
}

export type Errors = Record<string, string | undefined>;

export const hasErrors = (errors: Errors) => Object.values(errors).some(Boolean);

/** Serverio klaidų laukų pavadinimai (pvz., „passwordRepeatMatching“) suvienodinami su formos laukais. */
export function mapServerErrors(fieldErrors: Record<string, string>, aliases: Record<string, string> = {}): Errors {
  const result: Errors = {};
  for (const [field, message] of Object.entries(fieldErrors)) {
    result[aliases[field] ?? field] = message;
  }
  return result;
}
