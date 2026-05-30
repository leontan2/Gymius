# AI Food Calorie Scanner

## Feature Specification

### Summary

The AI Food Calorie Scanner lets authenticated Gymius users take or upload a food photo, receive an AI-generated nutrition estimate, correct the result, and save it to today's nutrition log.

The user flow must stay under three steps:

1. Camera: take or upload a meal photo.
2. Analysis: show the AI estimate, item list, range, confidence, and macros.
3. Confirm: allow correction, then save to today's nutrition log.

### Goals

- Add mobile-first meal capture from camera or gallery.
- Estimate total meal calories and optional macros.
- Let users edit the estimate before saving.
- Store only confirmed nutrition data by default.
- Do not permanently retain images unless the user explicitly opts in later.
- Show today's calories consumed against the user's daily target.

### Non-Goals

- Medical-grade calorie calculation.
- Automatic diet diagnosis or medical advice.
- Long-term image storage in the initial release.
- Barcode scanning.
- Full meal planning.

### User Stories

- As a user, I can tap "Snap Meal Photo" and open my camera on mobile.
- As a user, I can upload a saved image from my gallery or desktop.
- As a user, I can see a prominent result like "~450 kcal".
- As a user, I can see the likely foods detected in the image.
- As a user, I can correct the calorie estimate before saving.
- As a user, I can confirm the meal and have it counted in today's nutrition total.
- As a user, I can see today's calories consumed versus my daily target.

### UX Copy

- Capture CTA: "Snap Meal Photo"
- Upload fallback: "Upload from gallery"
- Loading state: "Analyzing your meal..."
- Success state: "Great snapshot! Your meal is estimated at ~450 kcal."
- Low confidence state: "This image is a little unclear. Estimate range: 400-500 kcal."
- Confirm CTA: "Add to today's log"
- Edit label: "Correct estimate"

### Functional Requirements

#### Camera and Upload

- Use the browser camera API through an HTML file input with `capture="environment"` for mobile-first behavior.
- Support desktop webcam capture as a progressive enhancement through `navigator.mediaDevices.getUserMedia`.
- Accept JPEG, PNG, and WebP.
- Enforce a max client-side upload size before submission.
- Compress images client-side before upload where possible.
- Allow users to retake or replace the image before analysis.

#### AI Analysis

- Send the image to the backend, never directly from Angular to the vendor API.
- Backend calls a food-focused image nutrition API, or a general vision model with a strict JSON prompt.
- The analysis response should include:
  - Estimated total calories.
  - Estimated calorie range.
  - Confidence level.
  - Detected food items.
  - Optional macros: protein, carbs, fat.
  - Portion notes.
  - Warnings for unclear images, multiple dishes, sauces, or hidden ingredients.

#### Confirmation and Logging

- The user must review before saving.
- The calorie estimate field must be editable.
- Food item names and macro fields should be editable in a later iteration; the first release can save the AI items as text.
- On confirmation, create a `NutritionEntry` for the logged-in user and today's date by default.
- Show today's total calories consumed versus daily target after save.

#### Privacy

- Images are used temporarily for analysis only.
- Store the uploaded image in memory or short-lived temporary object storage.
- Delete temporary images after analysis or after a short TTL.
- Store only confirmed structured data:
  - calories
  - macros
  - food item labels
  - confidence/range metadata
  - date/time
- Add explicit consent before implementing permanent image history.

### Data Model

#### NutritionEntry

```text
id: UUID
userId: UUID
entryDate: LocalDate
mealTime: Instant
source: MANUAL | AI_SCAN
foodItems: String
calories: Integer
calorieMin: Integer
calorieMax: Integer
proteinGrams: BigDecimal?
carbsGrams: BigDecimal?
fatGrams: BigDecimal?
confidence: LOW | MEDIUM | HIGH
notes: String?
createdAt: Instant
updatedAt: Instant
```

#### DailyNutritionGoal

```text
id: UUID
userId: UUID
dailyCalories: Integer
proteinGoalGrams: BigDecimal?
carbsGoalGrams: BigDecimal?
fatGoalGrams: BigDecimal?
createdAt: Instant
updatedAt: Instant
```

## Sample UI Wireframe Description

### Navigation

Add a new authenticated navigation item:

```text
Nutrition
```

### Nutrition Page Layout

Desktop:

```text
------------------------------------------------------------
Page header
Nutrition
Today: 1,240 / 2,200 kcal
[Snap Meal Photo]
------------------------------------------------------------

[Calories today card] [Protein card] [Carbs card] [Fat card]

------------------------------------------------------------
AI Food Calorie Scanner

Step 1: Camera
[Large camera/photo preview area]
[Snap Meal Photo] [Upload from gallery]

Step 2: Analysis
Great snapshot! Your meal is estimated at:
~450 kcal
Estimate range: 400-500 kcal
Detected: Grilled chicken, rice, broccoli
Macros: 38g protein, 46g carbs, 12g fat

Step 3: Confirm
Correct estimate: [450]
Notes: [optional]
[Add to today's log]
------------------------------------------------------------

Today's Nutrition Log
- 8:15 AM  Oats, banana             390 kcal
- 1:05 PM  Chicken, rice, broccoli  450 kcal
```

Mobile:

```text
Nutrition
1,240 / 2,200 kcal

[Snap Meal Photo]

[Photo preview]

~450 kcal
Great snapshot! Estimate range: 400-500 kcal.

Detected foods
Chicken
Rice
Broccoli

Correct estimate
[450]

[Add to today's log]
```

### Component Structure

```text
features/nutrition/
  nutrition-page.component.ts
  meal-scanner.component.ts
  nutrition-summary.component.ts
  nutrition-log.component.ts
```

## API Integration Plan

### Recommended Vendor Strategy

Use OpenAI as the primary meal analysis provider, behind a backend adapter interface so controllers stay stable if the provider changes later:

```java
public interface MealVisionClient {
    MealAnalysisResult analyze(MultipartFile image);
}
```

Initial provider:

- OpenAI Responses API with a vision-capable model.
- Send the image as a Base64 data URL from the backend to OpenAI.
- Ask for a strict structured JSON response matching `MealAnalysisResponse`.
- Treat the estimate as approximate and require user confirmation before saving.

Fallback provider options:

- Mock provider for local development and UI testing.
- Food-specific nutrition API only if future accuracy testing shows OpenAI estimates are not good enough for the product.
- Generic label detection APIs are not recommended for nutrition estimates because they identify image labels but do not calculate calories or macros.

### Candidate Integrations

#### OpenAI Responses API

Use for: multimodal image analysis with structured calorie, macro, item, and confidence output.

Backend flow:

1. Accept image upload from Angular.
2. Resize/compress if needed.
3. Strip EXIF metadata where possible.
4. Convert the image to a Base64 data URL.
5. Send the image and meal-estimation instructions to OpenAI.
6. Request structured JSON output.
7. Validate the returned object server-side.
8. Clamp obviously unrealistic values and mark confidence low when the image is incomplete or unclear.
9. Return a normalized `MealAnalysisResponse` to Angular.

Example request shape:

```json
{
  "model": "gpt-4.1-mini",
  "input": [
    {
      "role": "user",
      "content": [
        {
          "type": "input_text",
          "text": "Analyze this meal image and return the structured nutrition estimate."
        },
        {
          "type": "input_image",
          "image_url": "data:image/jpeg;base64,..."
        }
      ]
    }
  ],
  "text": {
    "format": {
      "type": "json_schema",
      "name": "meal_analysis",
      "strict": true,
      "schema": {}
    }
  }
}
```

The actual schema should be generated from or kept in sync with the backend DTO.

#### Mock provider

Use for: local development, tests, and demos without spending API credits.

Behavior:

1. Accept the uploaded file.
2. Return a deterministic sample response.
3. Never call OpenAI.

### Backend Endpoints

```text
POST /api/nutrition/analyze-image
Content-Type: multipart/form-data
Auth: required

Response:
{
  "estimatedCalories": 450,
  "calorieMin": 400,
  "calorieMax": 500,
  "confidence": "MEDIUM",
  "foodItems": ["Grilled chicken", "Rice", "Broccoli"],
  "proteinGrams": 38,
  "carbsGrams": 46,
  "fatGrams": 12,
  "notes": "Sauce is partially visible, so calories may be underestimated."
}
```

```text
POST /api/nutrition/entries
Auth: required

Request:
{
  "entryDate": "2026-05-30",
  "foodItems": "Grilled chicken, rice, broccoli",
  "calories": 450,
  "calorieMin": 400,
  "calorieMax": 500,
  "proteinGrams": 38,
  "carbsGrams": 46,
  "fatGrams": 12,
  "confidence": "MEDIUM",
  "source": "AI_SCAN",
  "notes": "Sauce is partially visible."
}
```

```text
GET /api/nutrition/today
Auth: required

Response:
{
  "date": "2026-05-30",
  "dailyCalorieTarget": 2200,
  "caloriesConsumed": 1240,
  "remainingCalories": 960,
  "entries": []
}
```

```text
PUT /api/nutrition/goals
Auth: required
```

### Angular Flow

1. User taps "Snap Meal Photo".
2. Angular opens camera/gallery input.
3. Angular shows preview and sends compressed image to `/api/nutrition/analyze-image`.
4. Angular displays analysis result.
5. User edits calorie estimate if needed.
6. Angular submits confirmed entry to `/api/nutrition/entries`.
7. Nutrition summary refreshes today's totals.

### Security and Limits

- Require authentication for all nutrition endpoints.
- Enforce ownership by logged-in user, same as workout data.
- Reject files over configured size.
- Reject unsupported content types.
- Strip EXIF metadata before sending to vendor when possible.
- Add rate limiting to image analysis endpoint.
- Do not log image payloads or full vendor responses containing image data.
- Store vendor API keys only in backend environment variables.

### Environment Variables

```text
MEAL_VISION_PROVIDER=openai|mock
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1-mini
OPENAI_IMAGE_DETAIL=auto
MEAL_IMAGE_MAX_BYTES=5242880
MEAL_IMAGE_RETENTION_SECONDS=0
DEFAULT_DAILY_CALORIES=2200
```

## Sample AI Inference Prompt

Use this prompt with the OpenAI Responses API and a vision-capable model. The backend should request structured output and validate the JSON before returning it to Angular.

```text
You are analyzing a meal photo for a fitness tracking app.

Task:
Identify visible food items, estimate portion sizes, and estimate total calories and macros.

Rules:
- Give approximate estimates, not medical advice.
- If the photo is unclear, partially occluded, or contains hidden ingredients, lower confidence and widen the calorie range.
- Account for visible sauces, oils, fried coatings, drinks, and mixed dishes.
- Do not claim certainty about ingredients that are not visible.
- If multiple dishes are visible, include each likely dish separately.
- Do not store or describe personal background details unrelated to the food.

Required JSON schema:
{
  "estimatedCalories": number,
  "calorieMin": number,
  "calorieMax": number,
  "confidence": "LOW" | "MEDIUM" | "HIGH",
  "foodItems": [
    {
      "name": string,
      "portionEstimate": string,
      "estimatedCalories": number
    }
  ],
  "proteinGrams": number | null,
  "carbsGrams": number | null,
  "fatGrams": number | null,
  "confidenceNote": string,
  "userMessage": string
}

Tone:
Encouraging and concise.

Example userMessage:
"Great snapshot! Your meal is estimated at ~450 kcal."
```

### Structured Output Schema

Use this as the OpenAI `text.format` JSON schema, or generate the schema from backend DTOs to avoid drift.

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": [
    "estimatedCalories",
    "calorieMin",
    "calorieMax",
    "confidence",
    "foodItems",
    "proteinGrams",
    "carbsGrams",
    "fatGrams",
    "confidenceNote",
    "userMessage"
  ],
  "properties": {
    "estimatedCalories": {
      "type": "integer"
    },
    "calorieMin": {
      "type": "integer"
    },
    "calorieMax": {
      "type": "integer"
    },
    "confidence": {
      "type": "string",
      "enum": ["LOW", "MEDIUM", "HIGH"]
    },
    "foodItems": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["name", "portionEstimate", "estimatedCalories"],
        "properties": {
          "name": {
            "type": "string"
          },
          "portionEstimate": {
            "type": "string"
          },
          "estimatedCalories": {
            "type": "integer"
          }
        }
      }
    },
    "proteinGrams": {
      "type": ["number", "null"]
    },
    "carbsGrams": {
      "type": ["number", "null"]
    },
    "fatGrams": {
      "type": ["number", "null"]
    },
    "confidenceNote": {
      "type": "string"
    },
    "userMessage": {
      "type": "string"
    }
  }
}
```

## Test Cases

### Functional Tests

1. Opens camera capture on mobile
   - Given the user is authenticated on mobile
   - When they tap "Snap Meal Photo"
   - Then the device camera/gallery chooser opens

2. Uploads gallery image
   - Given the user is on desktop
   - When they upload a JPEG meal image
   - Then the app shows a preview and starts analysis

3. Rejects unsupported file
   - Given the user selects a PDF
   - Then the app shows a validation error and does not call the backend

4. Shows analysis result
   - Given the provider returns calories, range, items, and macros
   - Then the app displays the prominent kcal estimate and detected items

5. Allows calorie correction
   - Given the AI result is 450 kcal
   - When the user changes it to 520 kcal and confirms
   - Then 520 kcal is saved

6. Saves to today's log
   - Given the user confirms a scan
   - Then a nutrition entry appears in today's log
   - And today's consumed calories increase by the confirmed amount

7. Protects user data
   - Given user A creates a nutrition entry
   - When user B requests entries
   - Then user B cannot see user A's entry

### Edge Case Tests

1. Multiple dishes
   - Image contains rice, curry, dessert, and drink
   - Expected: itemized response and wider calorie range

2. Unclear image
   - Image is blurry or too dark
   - Expected: low confidence and prompt to retake or manually adjust

3. Mixed sauces
   - Food has visible sauce or dressing
   - Expected: notes mention sauce uncertainty

4. Hidden ingredients
   - Sandwich, burrito, or covered bowl
   - Expected: lower confidence and wider range

5. Very large image
   - Image exceeds max size
   - Expected: client compresses or backend rejects with clear error

6. Provider timeout
   - Vision API does not respond
   - Expected: graceful error and option to retry or enter calories manually

7. Duplicate submit
   - User double-taps confirm
   - Expected: one saved entry only

### Accuracy Evaluation Tests

Use a small benchmark set of labeled meals:

```text
Simple plate: chicken breast, rice, broccoli
Mixed bowl: burrito bowl with sauce
Fast food: burger and fries
Breakfast: oats, banana, peanut butter
Restaurant meal: pasta with cream sauce
Drink: smoothie or milk tea
Dessert: cake slice
```

For each image, compare:

- AI calorie estimate vs. known approximate calories.
- Whether true food items appear in the detected list.
- Whether confidence is lower for hard images.
- Whether the final user-edit flow is fast and understandable.

Success criteria for MVP:

- At least 80% of benchmark meals have the correct primary food category.
- At least 70% of estimates fall within a practical user-facing range.
- Low-confidence images are flagged instead of presented as certain.
- Median camera-to-result time stays under 8 seconds on a typical mobile connection.

## Implementation Phases

### Phase 1: MVP

- Nutrition page and navigation.
- Camera/gallery capture.
- Backend upload endpoint.
- Mock provider adapter for local development.
- Confirm and save nutrition entry.
- Today's calorie total.

### Phase 2: Real AI Provider

- Add selected vendor adapter.
- Add rate limiting.
- Add EXIF stripping.
- Add provider response normalization.
- Add retry and timeout handling.

### Phase 3: Better Nutrition Tracking

- Daily calorie goals page.
- Macro goal tracking.
- Manual nutrition entries.
- Weekly nutrition trends.
- Optional user consent for saved meal images.
