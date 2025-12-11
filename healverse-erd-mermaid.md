# HealVerse Database ER Diagram (Mermaid)

```mermaid
erDiagram
    users {
        bigint id PK
        varchar username UK
        varchar password
        varchar email UK
        varchar google_id UK
        varchar profile_image
        timestamp created_at
        timestamp updated_at
    }

    user_profiles {
        bigint id PK
        bigint user_id FK
        enum gender
        int age
        decimal height_cm
        decimal current_weight_kg
        decimal target_weight_kg
        enum activity_level
        enum goal
        enum weight_loss_speed
        enum dietary_restriction
        enum health_conditions
        text other_health_condition_description
        timestamp created_at
        timestamp updated_at
    }

    conversations {
        varchar id PK
        bigint user_id FK
        varchar title
        timestamp created_at
        timestamp updated_at
    }

    messages {
        bigint id PK
        varchar conversation_id FK
        text content
        enum role
        timestamp created_at
    }

    diet_plans {
        bigint id PK
        bigint user_id FK
        date plan_date
        decimal total_calories
        decimal total_protein
        decimal total_carbs
        decimal total_fat
        boolean is_generated
        timestamp created_at
    }

    meals {
        bigint id PK
        bigint diet_plan_id FK
        enum meal_type
        varchar meal_name
        decimal calories
        decimal protein
        decimal carbs
        decimal fat
        int preparation_time_minutes
        text instructions
        text health_benefits
        json ingredients
        timestamp created_at
    }

    food_logs {
        bigint id PK
        bigint user_id FK
        enum meal_type
        varchar meal_name
        varchar image_url
        text image_description
        timestamp logged_at
        timestamp created_at
        boolean is_from_camera
    }

    food_items {
        bigint id PK
        bigint food_log_id FK
        varchar name
        double quantity
        varchar unit
        double calories
        double protein
        double fat
        double carbs
    }

    exercise_logs {
        bigint id PK
        bigint user_id FK
        varchar exercise_name
        int duration_minutes
        enum intensity
        decimal calories_burned
        timestamp logged_at
        timestamp created_at
    }

    water_logs {
        bigint id PK
        bigint user_id FK
        decimal amount_ml
        timestamp logged_at
        timestamp created_at
    }

    daily_nutrition_summaries {
        bigint id PK
        bigint user_id FK
        date date
        decimal target_calories
        decimal target_protein
        decimal target_carbs
        decimal target_fat
        decimal consumed_calories
        decimal consumed_protein
        decimal consumed_carbs
        decimal consumed_fat
        decimal calories_burned
        decimal water_consumed_ml
        decimal target_water_ml
        decimal remaining_calories
        timestamp created_at
        timestamp updated_at
    }

    medications {
        uuid id PK
        bigint user_id FK
        varchar name
        varchar dosage
        enum type
        enum frequency
        date start_date
        date end_date
        boolean is_active
        text notes
        timestamp created_at
        timestamp updated_at
    }

    medication_schedules {
        uuid id PK
        uuid medication_id FK
        time time
        boolean is_active
        timestamp created_at
    }

    medication_logs {
        uuid id PK
        uuid medication_id FK
        timestamp scheduled_time
        timestamp actual_time
        enum status
        text notes
        timestamp created_at
    }

    %% Relationships
    users ||--|| user_profiles : "has profile"
    users ||--o{ conversations : "has conversations"
    conversations ||--o{ messages : "contains messages"
    users ||--o{ diet_plans : "has diet plans"
    diet_plans ||--o{ meals : "contains meals"
    users ||--o{ food_logs : "logs food"
    food_logs ||--o{ food_items : "contains items"
    users ||--o{ exercise_logs : "logs exercises"
    users ||--o{ water_logs : "logs water intake"
    users ||--o{ daily_nutrition_summaries : "has summaries"
    users ||--o{ medications : "takes medications"
    medications ||--o{ medication_schedules : "has schedules"
    medications ||--o{ medication_logs : "has logs"
```

## How to View
1. Copy the mermaid code block
2. Paste it in GitHub README or GitLab documentation
3. Use VS Code with Mermaid extension
4. Visit [Mermaid Live Editor](https://mermaid-js.github.io/mermaid-live-editor/)
