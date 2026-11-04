export type Role = 'CLIENT' | 'DIETITIAN' | 'ADMIN';
export type Theme = 'LIGHT' | 'DARK' | 'SYSTEM';
export type FirstDayOfWeek = 'MONDAY' | 'SUNDAY';
export type Gender = 'MALE' | 'FEMALE';
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE';
export type Goal = 'LOSE_WEIGHT' | 'MAINTAIN' | 'GAIN_MUSCLE';
export type BmiCategory = 'UNDERWEIGHT' | 'NORMAL' | 'OVERWEIGHT' | 'OBESE';
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER';
export type ProductCategory =
  | 'MEAT_FISH'
  | 'DAIRY_EGGS'
  | 'GRAINS'
  | 'VEGETABLES'
  | 'FRUITS'
  | 'NUTS_SEEDS'
  | 'FATS_SPICES'
  | 'OTHER';
export type ReviewStatus = 'PENDING' | 'IN_REVIEW' | 'COMPLETED';
export type PaymentMethod = 'BANK_LINK' | 'CARD';
export type PaymentPurpose = 'SUBSCRIPTION' | 'REVIEW';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED';
export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'EXPIRED';

export interface Me {
  id: number;
  email: string;
  firstName: string;
  lastName: string | null;
  role: Role;
  mustChangePassword: boolean;
  theme: Theme;
  firstDayOfWeek: FirstDayOfWeek;
}

export interface Allergen {
  id: number;
  code: string;
  name: string;
}

export interface Product {
  id: number;
  name: string;
  category: ProductCategory;
}

export interface CalorieExplanation {
  bmr: number;
  activityFactor: number;
  afterActivity: number;
  goalFactor: number;
  calculatedCalories: number;
  safeMinimum: number;
  safeLimitApplied: boolean;
}

export interface Questionnaire {
  heightCm: number;
  weightKg: number;
  age: number;
  gender: Gender;
  activityLevel: ActivityLevel;
  goal: Goal;
  chronicDisease: boolean;
  allergens: Allergen[];
  dislikedProducts: Product[];
  updatedAt: string;
  metrics: {
    bmi: number;
    bmiCategory: BmiCategory;
    dailyCalories: number;
    explanation: CalorieExplanation;
  };
}

export interface QuestionnaireSaveResponse {
  questionnaire: Questionnaire;
  firstTime: boolean;
  chronicDiseaseWarning: boolean;
  safeLimitApplied: boolean;
}

export interface Nutrition {
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
}

export interface DishSummary {
  id: number;
  name: string;
  portionGrams: number;
  nutrition: Nutrition;
}

export interface Meal {
  type: MealType;
  targetCalories: number | null;
  dish: DishSummary;
}

export interface PlanDay {
  dayNumber: number;
  totalCalories: number;
  meals: Meal[];
}

export interface MealPlan {
  id: number;
  createdAt: string;
  active: boolean;
  aiGenerated: boolean;
  dailyCalories: number | null;
  days: PlanDay[];
}

export interface Recipe {
  planId: number;
  dishId: number;
  name: string;
  mealType: MealType;
  dayNumber: number;
  portionGrams: number;
  nutrition: Nutrition;
  ingredients: { productId: number; name: string; grams: number }[];
  steps: { number: number; description: string }[];
}

export interface ShoppingItem {
  id: number;
  productId: number;
  name: string;
  totalGrams: number;
  purchased: boolean;
}

export interface ShoppingList {
  planId: number;
  totalItems: number;
  purchasedItems: number;
  categories: { category: ProductCategory; items: ShoppingItem[] }[];
}

export interface GenerationStatus {
  canGenerate: boolean;
  reason: string | null;
  questionnaireFilled: boolean;
  subscriptionStatus: SubscriptionStatus;
  remainingToday: number;
  dailyLimit: number;
}

export interface Comment {
  id: number;
  dishId: number | null;
  dishName: string | null;
  text: string;
  createdAt: string;
}

export interface ClientReview {
  id: number;
  planId: number;
  status: ReviewStatus;
  question: string | null;
  submittedAt: string;
  reviewedAt: string | null;
  dietitianName: string | null;
  comments: Comment[];
}

export interface OrderInfo {
  price: number;
  planId: number | null;
  canOrder: boolean;
  reason: string | null;
}

export interface PaymentInitiated {
  reference: string;
  redirectUrl: string;
}

export interface Payment {
  reference: string;
  amount: number;
  purpose: PaymentPurpose;
  method: PaymentMethod;
  status: PaymentStatus;
  createdAt: string;
}

export interface Subscription {
  status: SubscriptionStatus;
  validUntil: string;
  trialEndsAt: string;
  paidUntil: string | null;
  price: number;
  canPay: boolean;
}

export interface Notification {
  id: number;
  message: string;
  createdAt: string;
  read: boolean;
}

export interface Settings {
  theme: Theme;
  firstDayOfWeek: FirstDayOfWeek;
}

export interface DietitianReviewSummary {
  id: number;
  status: ReviewStatus;
  submittedAt: string;
  question: string | null;
  assignedToMe: boolean;
}

export interface DietitianReviewDetail {
  id: number;
  status: ReviewStatus;
  submittedAt: string;
  reviewedAt: string | null;
  question: string | null;
  questionnaire: Questionnaire | null;
  plan: MealPlan;
  comments: Comment[];
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string | null;
  role: Role;
  blocked: boolean;
  createdAt: string;
}
