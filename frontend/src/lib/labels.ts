import type {
  ActivityLevel,
  BmiCategory,
  Gender,
  Goal,
  MealType,
  PaymentMethod,
  ProductCategory,
  ReviewStatus,
  Role,
  SubscriptionStatus,
} from '../api/types';

/** Sąsajos sąvokos lietuvių kalba (16 lentelė, UI-17). */

export const genderLabels: Record<Gender, string> = { MALE: 'Vyras', FEMALE: 'Moteris' };

export const activityLabels: Record<ActivityLevel, { name: string; hint: string; factor: number }> = {
  SEDENTARY: { name: 'Sėslus', hint: 'Sėdimas darbas, beveik nesportuojate', factor: 1.2 },
  LIGHT: { name: 'Lengvas', hint: 'Sportas 1–3 k. per savaitę', factor: 1.375 },
  MODERATE: { name: 'Vidutinis', hint: 'Sportas 3–5 k. per savaitę', factor: 1.55 },
  ACTIVE: { name: 'Didelis', hint: 'Sportas 6–7 k. per savaitę', factor: 1.725 },
  VERY_ACTIVE: { name: 'Labai didelis', hint: 'Fizinis darbas ir kasdienis sportas', factor: 1.9 },
};

export const goalLabels: Record<Goal, string> = {
  LOSE_WEIGHT: 'Sulieknėti',
  MAINTAIN: 'Palaikyti svorį',
  GAIN_MUSCLE: 'Priaugti raumenų masės',
};

export const bmiCategoryLabels: Record<BmiCategory, string> = {
  UNDERWEIGHT: 'Per mažas svoris',
  NORMAL: 'Normalus svoris',
  OVERWEIGHT: 'Antsvoris',
  OBESE: 'Nutukimas',
};

export const mealTypeLabels: Record<MealType, string> = {
  BREAKFAST: 'Pusryčiai',
  LUNCH: 'Pietūs',
  DINNER: 'Vakarienė',
};

export const productCategoryLabels: Record<ProductCategory, string> = {
  MEAT_FISH: 'Mėsa ir žuvis',
  DAIRY_EGGS: 'Pieno produktai ir kiaušiniai',
  GRAINS: 'Grūdai ir ankštiniai',
  VEGETABLES: 'Daržovės',
  FRUITS: 'Vaisiai ir uogos',
  NUTS_SEEDS: 'Riešutai ir sėklos',
  FATS_SPICES: 'Riebalai ir prieskoniai',
  OTHER: 'Kita',
};

export const reviewStatusLabels: Record<ReviewStatus, string> = {
  PENDING: 'Laukia',
  IN_REVIEW: 'Peržiūrima',
  COMPLETED: 'Atlikta',
};

export const paymentMethodLabels: Record<PaymentMethod, string> = {
  BANK_LINK: 'Elektroninė bankininkystė',
  CARD: 'Mokėjimo kortelė',
};

export const subscriptionStatusLabels: Record<SubscriptionStatus, string> = {
  TRIAL: 'Bandomasis laikotarpis',
  ACTIVE: 'Aktyvi',
  EXPIRED: 'Pasibaigusi',
};

export const roleLabels: Record<Role, string> = {
  CLIENT: 'Klientas',
  DIETITIAN: 'Dietologas',
  ADMIN: 'Administratorius',
};
