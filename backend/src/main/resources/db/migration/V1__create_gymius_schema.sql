CREATE TABLE users (
    id UUID NOT NULL,
    google_subject VARCHAR(128) NOT NULL,
    email VARCHAR(320) NOT NULL,
    name VARCHAR(160) NOT NULL,
    picture_url VARCHAR(1000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_google_subject UNIQUE (google_subject),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE workouts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    workout_date DATE NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_workouts PRIMARY KEY (id),
    CONSTRAINT fk_workouts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_workouts_user_date ON workouts (user_id, workout_date);

CREATE TABLE exercise_logs (
    id UUID NOT NULL,
    workout_id UUID NOT NULL,
    exercise_name VARCHAR(120) NOT NULL,
    sets_count INTEGER NOT NULL,
    reps INTEGER NOT NULL,
    weight NUMERIC(8, 2) NOT NULL,
    notes VARCHAR(500),
    sort_order INTEGER NOT NULL,
    CONSTRAINT pk_exercise_logs PRIMARY KEY (id),
    CONSTRAINT fk_exercise_logs_workout FOREIGN KEY (workout_id) REFERENCES workouts (id) ON DELETE CASCADE
);

CREATE INDEX idx_exercise_logs_workout ON exercise_logs (workout_id);
CREATE INDEX idx_exercise_logs_name ON exercise_logs (exercise_name);

CREATE TABLE nutrition_entries (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    entry_date DATE NOT NULL,
    meal_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    source VARCHAR(40) NOT NULL,
    food_items VARCHAR(1000) NOT NULL,
    calories INTEGER NOT NULL,
    calorie_min INTEGER,
    calorie_max INTEGER,
    protein_grams NUMERIC(8, 2),
    carbs_grams NUMERIC(8, 2),
    fat_grams NUMERIC(8, 2),
    confidence VARCHAR(20) NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_nutrition_entries PRIMARY KEY (id),
    CONSTRAINT fk_nutrition_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_nutrition_entries_confidence CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX idx_nutrition_entries_user_date ON nutrition_entries (user_id, entry_date);

CREATE TABLE daily_nutrition_goals (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    daily_calories INTEGER NOT NULL,
    protein_goal_grams NUMERIC(8, 2),
    carbs_goal_grams NUMERIC(8, 2),
    fat_goal_grams NUMERIC(8, 2),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_daily_nutrition_goals PRIMARY KEY (id),
    CONSTRAINT uk_daily_nutrition_goals_user UNIQUE (user_id),
    CONSTRAINT fk_daily_nutrition_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
