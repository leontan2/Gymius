export interface UserProfile {
  id: string;
  name: string;
  email: string;
  pictureUrl: string | null;
}

export interface ExerciseLog {
  id?: string;
  exerciseName: string;
  sets: number;
  reps: number;
  weight: number;
  notes?: string | null;
  sortOrder?: number;
  volume?: number;
}

export interface Workout {
  id: string;
  workoutDate: string;
  notes: string | null;
  exercises: ExerciseLog[];
  totalVolume: number;
  createdAt: string;
  updatedAt: string;
}

export interface WorkoutRequest {
  workoutDate: string;
  notes: string | null;
  exercises: ExerciseLog[];
}

export interface WorkoutSummary {
  id: string;
  workoutDate: string;
  exerciseCount: number;
  totalVolume: number;
}

export interface Dashboard {
  recentWorkouts: WorkoutSummary[];
  weeklyWorkoutCount: number;
  totalWorkouts: number;
  totalVolumeLifted: number;
}

export interface ProgressPoint {
  date: string;
  maxWeight: number;
  volume: number;
}

export interface ProgressSeries {
  exerciseName: string;
  points: ProgressPoint[];
}

export interface PersonalRecord {
  exerciseName: string;
  highestWeight: number;
  repsAtHighestWeight: number;
  highestWeightDate: string;
  highestWeightWorkoutId: string;
  highestReps: number;
  weightAtHighestReps: number;
  highestRepsDate: string;
  highestRepsWorkoutId: string;
}

export type NutritionConfidence = 'LOW' | 'MEDIUM' | 'HIGH';

export interface MealFoodItem {
  name: string;
  portionEstimate: string;
  estimatedCalories: number;
}

export interface MealAnalysis {
  estimatedCalories: number;
  calorieMin: number;
  calorieMax: number;
  confidence: NutritionConfidence;
  foodItems: MealFoodItem[];
  proteinGrams: number | null;
  carbsGrams: number | null;
  fatGrams: number | null;
  confidenceNote: string;
  userMessage: string;
}

export interface NutritionEntry {
  id: string;
  entryDate: string;
  mealTime: string;
  source: string;
  foodItems: string;
  calories: number;
  calorieMin: number | null;
  calorieMax: number | null;
  proteinGrams: number | null;
  carbsGrams: number | null;
  fatGrams: number | null;
  confidence: NutritionConfidence;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NutritionEntryRequest {
  entryDate: string;
  foodItems: string;
  calories: number;
  calorieMin: number | null;
  calorieMax: number | null;
  proteinGrams: number | null;
  carbsGrams: number | null;
  fatGrams: number | null;
  confidence: NutritionConfidence;
  notes: string | null;
}

export interface NutritionGoal {
  dailyCalories: number;
  proteinGoalGrams: number | null;
  carbsGoalGrams: number | null;
  fatGoalGrams: number | null;
}

export interface NutritionToday {
  date: string;
  goal: NutritionGoal;
  caloriesConsumed: number;
  remainingCalories: number;
  proteinGrams: number;
  carbsGrams: number;
  fatGrams: number;
  entries: NutritionEntry[];
}
