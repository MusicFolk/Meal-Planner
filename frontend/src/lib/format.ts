import type { FirstDayOfWeek } from '../api/types';

/** Formatavimas pagal UI-17 ir NFR-19, NFR-20: datos MMMM-MM-DD, dešimtainis skyriklis – kablelis. */

const integer = new Intl.NumberFormat('lt-LT', { maximumFractionDigits: 0 });
const oneDecimal = new Intl.NumberFormat('lt-LT', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const factor = new Intl.NumberFormat('lt-LT', { minimumFractionDigits: 1, maximumFractionDigits: 3 });
const money = new Intl.NumberFormat('lt-LT', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export const formatKcal = (kcal: number) => `${integer.format(Math.round(kcal))} kcal`;
export const formatGrams = (grams: number) => `${integer.format(Math.round(grams))} g`;
export const formatInt = (value: number) => integer.format(Math.round(value));
export const formatDecimal = (value: number) => oneDecimal.format(value);
/** Koeficientai rodomi be apvalinimo: 1,2; 1,375; 0,85. */
export const formatFactor = (value: number) => factor.format(value);
export const formatMoney = (value: number) => `${money.format(value)} €`;

export function formatDate(iso: string | Date): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${formatDate(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const MONTHS_GENITIVE = [
  'sausio',
  'vasario',
  'kovo',
  'balandžio',
  'gegužės',
  'birželio',
  'liepos',
  'rugpjūčio',
  'rugsėjo',
  'spalio',
  'lapkričio',
  'gruodžio',
];

/** Plano dienos numeris 1–7 atitinka pirmadienį–sekmadienį. */
export const WEEKDAYS = [
  { short: 'Pr', full: 'Pirmadienis' },
  { short: 'An', full: 'Antradienis' },
  { short: 'Tr', full: 'Trečiadienis' },
  { short: 'Kt', full: 'Ketvirtadienis' },
  { short: 'Pn', full: 'Penktadienis' },
  { short: 'Š', full: 'Šeštadienis' },
  { short: 'S', full: 'Sekmadienis' },
];

export interface PlanCalendarDay {
  dayNumber: number;
  date: Date;
  short: string;
  full: string;
}

/**
 * Plano dienų išdėstymas pagal kliento pasirinktą savaitės pirmąją dieną (UI-21, FR-17).
 * Datos skaičiuojamos savaitei, kurioje planas sudarytas.
 */
export function planCalendar(createdAt: string, firstDay: FirstDayOfWeek): PlanCalendarDay[] {
  const created = new Date(createdAt);
  created.setHours(0, 0, 0, 0);
  const weekday = (created.getDay() + 6) % 7; // 0 = pirmadienis
  const order = firstDay === 'SUNDAY' ? [7, 1, 2, 3, 4, 5, 6] : [1, 2, 3, 4, 5, 6, 7];
  const start = new Date(created);
  if (firstDay === 'SUNDAY') {
    start.setDate(created.getDate() - ((weekday + 1) % 7));
  } else {
    start.setDate(created.getDate() - weekday);
  }
  return order.map((dayNumber, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return { dayNumber, date, ...WEEKDAYS[dayNumber - 1] };
  });
}

/** Antraštės formatas: „spalio 19–25 d.“ arba „spalio 26 – lapkričio 1 d.“ (UI-17). */
export function formatWeekRange(from: Date, to: Date): string {
  if (from.getMonth() === to.getMonth()) {
    return `${MONTHS_GENITIVE[from.getMonth()]} ${from.getDate()}–${to.getDate()} d.`;
  }
  return `${MONTHS_GENITIVE[from.getMonth()]} ${from.getDate()} – ${MONTHS_GENITIVE[to.getMonth()]} ${to.getDate()} d.`;
}

export function formatDayHeading(day: PlanCalendarDay): string {
  return `${day.full}, ${MONTHS_GENITIVE[day.date.getMonth()]} ${day.date.getDate()} d.`;
}
