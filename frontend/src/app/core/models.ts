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
