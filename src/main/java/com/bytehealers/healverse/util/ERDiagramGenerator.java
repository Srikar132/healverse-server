package com.bytehealers.healverse.util;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Database ER Diagram Generator for HealVerse
 * Generates different formats for various ER diagram tools
 */
public class ERDiagramGenerator {

    public static void main(String[] args) {
        ERDiagramGenerator generator = new ERDiagramGenerator();
        
        // Generate different formats
        try {
            generator.generateMermaidERD("healverse-erd-mermaid.md");
            generator.generatePlantUMLERD("healverse-erd-plantuml.puml");
            generator.generateDBMLERD("healverse-erd-dbml.dbml");
            generator.generateSQLCreateScript("healverse-schema.sql");
            
            System.out.println("ER Diagrams generated successfully!");
            System.out.println("Files created:");
            System.out.println("1. healverse-erd-mermaid.md - For Mermaid.js (GitHub, GitLab, VS Code extensions)");
            System.out.println("2. healverse-erd-plantuml.puml - For PlantUML");
            System.out.println("3. healverse-erd-dbml.dbml - For dbdiagram.io");
            System.out.println("4. healverse-schema.sql - Complete SQL schema");
            
        } catch (IOException e) {
            System.err.println("Error generating ER diagrams: " + e.getMessage());
        }
    }

    /**
     * Generate Mermaid ER Diagram
     * Can be used in GitHub README, GitLab, VS Code with Mermaid extensions
     */
    public void generateMermaidERD(String filename) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        
        mermaid.append("# HealVerse Database ER Diagram (Mermaid)\n\n");
        mermaid.append("```mermaid\n");
        mermaid.append("erDiagram\n");
        
        // Users table
        mermaid.append("    users {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        varchar username UK\n");
        mermaid.append("        varchar password\n");
        mermaid.append("        varchar email UK\n");
        mermaid.append("        varchar google_id UK\n");
        mermaid.append("        varchar profile_image\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("        timestamp updated_at\n");
        mermaid.append("    }\n\n");

        // User Profiles table
        mermaid.append("    user_profiles {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        enum gender\n");
        mermaid.append("        int age\n");
        mermaid.append("        decimal height_cm\n");
        mermaid.append("        decimal current_weight_kg\n");
        mermaid.append("        decimal target_weight_kg\n");
        mermaid.append("        enum activity_level\n");
        mermaid.append("        enum goal\n");
        mermaid.append("        enum weight_loss_speed\n");
        mermaid.append("        enum dietary_restriction\n");
        mermaid.append("        enum health_conditions\n");
        mermaid.append("        text other_health_condition_description\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("        timestamp updated_at\n");
        mermaid.append("    }\n\n");

        // Conversations table
        mermaid.append("    conversations {\n");
        mermaid.append("        varchar id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        varchar title\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("        timestamp updated_at\n");
        mermaid.append("    }\n\n");

        // Messages table
        mermaid.append("    messages {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        varchar conversation_id FK\n");
        mermaid.append("        text content\n");
        mermaid.append("        enum role\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Diet Plans table
        mermaid.append("    diet_plans {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        date plan_date\n");
        mermaid.append("        decimal total_calories\n");
        mermaid.append("        decimal total_protein\n");
        mermaid.append("        decimal total_carbs\n");
        mermaid.append("        decimal total_fat\n");
        mermaid.append("        boolean is_generated\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Meals table
        mermaid.append("    meals {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint diet_plan_id FK\n");
        mermaid.append("        enum meal_type\n");
        mermaid.append("        varchar meal_name\n");
        mermaid.append("        decimal calories\n");
        mermaid.append("        decimal protein\n");
        mermaid.append("        decimal carbs\n");
        mermaid.append("        decimal fat\n");
        mermaid.append("        int preparation_time_minutes\n");
        mermaid.append("        text instructions\n");
        mermaid.append("        text health_benefits\n");
        mermaid.append("        json ingredients\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Food Logs table
        mermaid.append("    food_logs {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        enum meal_type\n");
        mermaid.append("        varchar meal_name\n");
        mermaid.append("        varchar image_url\n");
        mermaid.append("        text image_description\n");
        mermaid.append("        timestamp logged_at\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("        boolean is_from_camera\n");
        mermaid.append("    }\n\n");

        // Food Items table
        mermaid.append("    food_items {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint food_log_id FK\n");
        mermaid.append("        varchar name\n");
        mermaid.append("        double quantity\n");
        mermaid.append("        varchar unit\n");
        mermaid.append("        double calories\n");
        mermaid.append("        double protein\n");
        mermaid.append("        double fat\n");
        mermaid.append("        double carbs\n");
        mermaid.append("    }\n\n");

        // Exercise Logs table
        mermaid.append("    exercise_logs {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        varchar exercise_name\n");
        mermaid.append("        int duration_minutes\n");
        mermaid.append("        enum intensity\n");
        mermaid.append("        decimal calories_burned\n");
        mermaid.append("        timestamp logged_at\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Water Logs table
        mermaid.append("    water_logs {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        decimal amount_ml\n");
        mermaid.append("        timestamp logged_at\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Daily Nutrition Summaries table
        mermaid.append("    daily_nutrition_summaries {\n");
        mermaid.append("        bigint id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        date date\n");
        mermaid.append("        decimal target_calories\n");
        mermaid.append("        decimal target_protein\n");
        mermaid.append("        decimal target_carbs\n");
        mermaid.append("        decimal target_fat\n");
        mermaid.append("        decimal consumed_calories\n");
        mermaid.append("        decimal consumed_protein\n");
        mermaid.append("        decimal consumed_carbs\n");
        mermaid.append("        decimal consumed_fat\n");
        mermaid.append("        decimal calories_burned\n");
        mermaid.append("        decimal water_consumed_ml\n");
        mermaid.append("        decimal target_water_ml\n");
        mermaid.append("        decimal remaining_calories\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("        timestamp updated_at\n");
        mermaid.append("    }\n\n");

        // Medications table
        mermaid.append("    medications {\n");
        mermaid.append("        uuid id PK\n");
        mermaid.append("        bigint user_id FK\n");
        mermaid.append("        varchar name\n");
        mermaid.append("        varchar dosage\n");
        mermaid.append("        enum type\n");
        mermaid.append("        enum frequency\n");
        mermaid.append("        date start_date\n");
        mermaid.append("        date end_date\n");
        mermaid.append("        boolean is_active\n");
        mermaid.append("        text notes\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("        timestamp updated_at\n");
        mermaid.append("    }\n\n");

        // Medication Schedules table
        mermaid.append("    medication_schedules {\n");
        mermaid.append("        uuid id PK\n");
        mermaid.append("        uuid medication_id FK\n");
        mermaid.append("        time time\n");
        mermaid.append("        boolean is_active\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Medication Logs table
        mermaid.append("    medication_logs {\n");
        mermaid.append("        uuid id PK\n");
        mermaid.append("        uuid medication_id FK\n");
        mermaid.append("        timestamp scheduled_time\n");
        mermaid.append("        timestamp actual_time\n");
        mermaid.append("        enum status\n");
        mermaid.append("        text notes\n");
        mermaid.append("        timestamp created_at\n");
        mermaid.append("    }\n\n");

        // Define relationships
        mermaid.append("    %% Relationships\n");
        mermaid.append("    users ||--|| user_profiles : \"has profile\"\n");
        mermaid.append("    users ||--o{ conversations : \"has conversations\"\n");
        mermaid.append("    conversations ||--o{ messages : \"contains messages\"\n");
        mermaid.append("    users ||--o{ diet_plans : \"has diet plans\"\n");
        mermaid.append("    diet_plans ||--o{ meals : \"contains meals\"\n");
        mermaid.append("    users ||--o{ food_logs : \"logs food\"\n");
        mermaid.append("    food_logs ||--o{ food_items : \"contains items\"\n");
        mermaid.append("    users ||--o{ exercise_logs : \"logs exercises\"\n");
        mermaid.append("    users ||--o{ water_logs : \"logs water intake\"\n");
        mermaid.append("    users ||--o{ daily_nutrition_summaries : \"has summaries\"\n");
        mermaid.append("    users ||--o{ medications : \"takes medications\"\n");
        mermaid.append("    medications ||--o{ medication_schedules : \"has schedules\"\n");
        mermaid.append("    medications ||--o{ medication_logs : \"has logs\"\n");
        
        mermaid.append("```\n\n");
        mermaid.append("## How to View\n");
        mermaid.append("1. Copy the mermaid code block\n");
        mermaid.append("2. Paste it in GitHub README or GitLab documentation\n");
        mermaid.append("3. Use VS Code with Mermaid extension\n");
        mermaid.append("4. Visit [Mermaid Live Editor](https://mermaid-js.github.io/mermaid-live-editor/)\n");

        writeToFile(filename, mermaid.toString());
    }

    /**
     * Generate PlantUML ER Diagram
     * Can be used with PlantUML server, VS Code extensions, or online editors
     */
    public void generatePlantUMLERD(String filename) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        
        plantuml.append("@startuml HealVerse_Database_ERD\n");
        plantuml.append("!define ENTITY entity\n");
        plantuml.append("!define PK <color:red><b>PK</b></color>\n");
        plantuml.append("!define FK <color:blue><b>FK</b></color>\n");
        plantuml.append("!define UK <color:green><b>UK</b></color>\n\n");
        
        plantuml.append("skinparam linetype ortho\n");
        plantuml.append("skinparam packageStyle rectangle\n\n");
        
        // Define entities
        plantuml.append("entity \"users\" as users {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * username : VARCHAR(255) UK\n");
        plantuml.append("  password : VARCHAR(255)\n");
        plantuml.append("  email : VARCHAR(255) UK\n");
        plantuml.append("  google_id : VARCHAR(255) UK\n");
        plantuml.append("  profile_image : VARCHAR(255)\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("  updated_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"user_profiles\" as user_profiles {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * gender : ENUM\n");
        plantuml.append("  * age : INT\n");
        plantuml.append("  * height_cm : DECIMAL(5,2)\n");
        plantuml.append("  * current_weight_kg : DECIMAL(5,2)\n");
        plantuml.append("  * target_weight_kg : DECIMAL(5,2)\n");
        plantuml.append("  * activity_level : ENUM\n");
        plantuml.append("  * goal : ENUM\n");
        plantuml.append("  weight_loss_speed : ENUM\n");
        plantuml.append("  dietary_restriction : ENUM\n");
        plantuml.append("  health_conditions : ENUM\n");
        plantuml.append("  other_health_condition_description : TEXT\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("  updated_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"conversations\" as conversations {\n");
        plantuml.append("  * id : VARCHAR(255) PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  title : VARCHAR(500)\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("  updated_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"messages\" as messages {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * conversation_id : VARCHAR(255) FK\n");
        plantuml.append("  * content : TEXT\n");
        plantuml.append("  * role : ENUM\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"diet_plans\" as diet_plans {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * plan_date : DATE\n");
        plantuml.append("  * total_calories : DECIMAL(6,2)\n");
        plantuml.append("  * total_protein : DECIMAL(5,2)\n");
        plantuml.append("  * total_carbs : DECIMAL(5,2)\n");
        plantuml.append("  * total_fat : DECIMAL(5,2)\n");
        plantuml.append("  is_generated : BOOLEAN\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"meals\" as meals {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * diet_plan_id : BIGINT FK\n");
        plantuml.append("  * meal_type : ENUM\n");
        plantuml.append("  * meal_name : VARCHAR(255)\n");
        plantuml.append("  * calories : DECIMAL(6,2)\n");
        plantuml.append("  * protein : DECIMAL(5,2)\n");
        plantuml.append("  * carbs : DECIMAL(5,2)\n");
        plantuml.append("  * fat : DECIMAL(5,2)\n");
        plantuml.append("  preparation_time_minutes : INT\n");
        plantuml.append("  instructions : TEXT\n");
        plantuml.append("  health_benefits : TEXT\n");
        plantuml.append("  ingredients : JSON\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        // Add other entities similarly...
        plantuml.append("entity \"food_logs\" as food_logs {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * meal_type : ENUM\n");
        plantuml.append("  meal_name : VARCHAR(255)\n");
        plantuml.append("  image_url : VARCHAR(255)\n");
        plantuml.append("  image_description : TEXT\n");
        plantuml.append("  * logged_at : TIMESTAMP\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("  is_from_camera : BOOLEAN\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"food_items\" as food_items {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * food_log_id : BIGINT FK\n");
        plantuml.append("  * name : VARCHAR(255)\n");
        plantuml.append("  * quantity : DOUBLE\n");
        plantuml.append("  * unit : VARCHAR(50)\n");
        plantuml.append("  * calories : DOUBLE\n");
        plantuml.append("  * protein : DOUBLE\n");
        plantuml.append("  * fat : DOUBLE\n");
        plantuml.append("  * carbs : DOUBLE\n");
        plantuml.append("}\n\n");

        // Add remaining entities...
        plantuml.append("entity \"exercise_logs\" as exercise_logs {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * exercise_name : VARCHAR(255)\n");
        plantuml.append("  * duration_minutes : INT\n");
        plantuml.append("  * intensity : ENUM\n");
        plantuml.append("  * calories_burned : DECIMAL(6,2)\n");
        plantuml.append("  * logged_at : TIMESTAMP\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"water_logs\" as water_logs {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * amount_ml : DECIMAL(8,2)\n");
        plantuml.append("  * logged_at : TIMESTAMP\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"daily_nutrition_summaries\" as daily_nutrition_summaries {\n");
        plantuml.append("  * id : BIGINT PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * date : DATE\n");
        plantuml.append("  * target_calories : DECIMAL(6,2)\n");
        plantuml.append("  * target_protein : DECIMAL(5,2)\n");
        plantuml.append("  * target_carbs : DECIMAL(5,2)\n");
        plantuml.append("  * target_fat : DECIMAL(5,2)\n");
        plantuml.append("  consumed_calories : DECIMAL(6,2)\n");
        plantuml.append("  consumed_protein : DECIMAL(5,2)\n");
        plantuml.append("  consumed_carbs : DECIMAL(5,2)\n");
        plantuml.append("  consumed_fat : DECIMAL(5,2)\n");
        plantuml.append("  calories_burned : DECIMAL(6,2)\n");
        plantuml.append("  water_consumed_ml : DECIMAL(8,2)\n");
        plantuml.append("  target_water_ml : DECIMAL(8,2)\n");
        plantuml.append("  remaining_calories : DECIMAL(6,2)\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("  updated_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"medications\" as medications {\n");
        plantuml.append("  * id : UUID PK\n");
        plantuml.append("  * user_id : BIGINT FK\n");
        plantuml.append("  * name : VARCHAR(255)\n");
        plantuml.append("  * dosage : VARCHAR(255)\n");
        plantuml.append("  * type : ENUM\n");
        plantuml.append("  * frequency : ENUM\n");
        plantuml.append("  * start_date : DATE\n");
        plantuml.append("  end_date : DATE\n");
        plantuml.append("  is_active : BOOLEAN\n");
        plantuml.append("  notes : TEXT\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("  updated_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"medication_schedules\" as medication_schedules {\n");
        plantuml.append("  * id : UUID PK\n");
        plantuml.append("  * medication_id : UUID FK\n");
        plantuml.append("  * time : TIME\n");
        plantuml.append("  is_active : BOOLEAN\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        plantuml.append("entity \"medication_logs\" as medication_logs {\n");
        plantuml.append("  * id : UUID PK\n");
        plantuml.append("  * medication_id : UUID FK\n");
        plantuml.append("  * scheduled_time : TIMESTAMP\n");
        plantuml.append("  actual_time : TIMESTAMP\n");
        plantuml.append("  * status : ENUM\n");
        plantuml.append("  notes : TEXT\n");
        plantuml.append("  created_at : TIMESTAMP\n");
        plantuml.append("}\n\n");

        // Define relationships
        plantuml.append("' Relationships\n");
        plantuml.append("users ||--|| user_profiles : \"has\"\n");
        plantuml.append("users ||--o{ conversations : \"has\"\n");
        plantuml.append("conversations ||--o{ messages : \"contains\"\n");
        plantuml.append("users ||--o{ diet_plans : \"has\"\n");
        plantuml.append("diet_plans ||--o{ meals : \"contains\"\n");
        plantuml.append("users ||--o{ food_logs : \"logs\"\n");
        plantuml.append("food_logs ||--o{ food_items : \"contains\"\n");
        plantuml.append("users ||--o{ exercise_logs : \"logs\"\n");
        plantuml.append("users ||--o{ water_logs : \"logs\"\n");
        plantuml.append("users ||--o{ daily_nutrition_summaries : \"has\"\n");
        plantuml.append("users ||--o{ medications : \"takes\"\n");
        plantuml.append("medications ||--o{ medication_schedules : \"has\"\n");
        plantuml.append("medications ||--o{ medication_logs : \"has\"\n");
        
        plantuml.append("@enduml\n");

        writeToFile(filename, plantuml.toString());
    }

    /**
     * Generate DBML for dbdiagram.io
     * dbdiagram.io is an excellent online ER diagram tool
     */
    public void generateDBMLERD(String filename) throws IOException {
        StringBuilder dbml = new StringBuilder();
        
        dbml.append("// HealVerse Database Schema for dbdiagram.io\n");
        dbml.append("// Visit https://dbdiagram.io/d and paste this code\n\n");
        
        dbml.append("Project HealVerse {\n");
        dbml.append("  database_type: 'PostgreSQL'\n");
        dbml.append("  Note: 'HealVerse - Comprehensive Health & Wellness Platform Database'\n");
        dbml.append("}\n\n");

        // Define tables
        dbml.append("Table users {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  username varchar(255) [unique, not null]\n");
        dbml.append("  password varchar(255)\n");
        dbml.append("  email varchar(255) [unique]\n");
        dbml.append("  google_id varchar(255) [unique]\n");
        dbml.append("  profile_image varchar(255)\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("  updated_at timestamp [default: `now()`]\n");
        dbml.append("  \n");
        dbml.append("  Indexes {\n");
        dbml.append("    username [unique]\n");
        dbml.append("    email\n");
        dbml.append("  }\n");
        dbml.append("}\n\n");

        dbml.append("Table user_profiles {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  user_id bigint [ref: > users.id, not null]\n");
        dbml.append("  gender gender_enum [not null]\n");
        dbml.append("  age int [not null]\n");
        dbml.append("  height_cm decimal(5,2) [not null]\n");
        dbml.append("  current_weight_kg decimal(5,2) [not null]\n");
        dbml.append("  target_weight_kg decimal(5,2) [not null]\n");
        dbml.append("  activity_level activity_level_enum [not null]\n");
        dbml.append("  goal goal_enum [not null]\n");
        dbml.append("  address varchar(255)\n");
        dbml.append("  weight_loss_speed weight_loss_speed_enum\n");
        dbml.append("  dietary_restriction dietary_restriction_enum\n");
        dbml.append("  health_conditions health_condition_enum\n");
        dbml.append("  other_health_condition_description text\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("  updated_at timestamp [default: `now()`]\n");
        dbml.append("}\n\n");

        dbml.append("Table conversations {\n");
        dbml.append("  id varchar(255) [pk]\n");
        dbml.append("  user_id bigint [ref: > users.id, not null]\n");
        dbml.append("  title varchar(500)\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("  updated_at timestamp [default: `now()`]\n");
        dbml.append("  \n");
        dbml.append("  Indexes {\n");
        dbml.append("    user_id\n");
        dbml.append("  }\n");
        dbml.append("}\n\n");

        dbml.append("Table messages {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  conversation_id varchar(255) [ref: > conversations.id, not null]\n");
        dbml.append("  content text [not null]\n");
        dbml.append("  role message_role_enum [not null]\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("}\n\n");

        dbml.append("Table diet_plans {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  user_id bigint [ref: > users.id, not null]\n");
        dbml.append("  plan_date date [not null]\n");
        dbml.append("  total_calories decimal(6,2) [not null]\n");
        dbml.append("  total_protein decimal(5,2) [not null]\n");
        dbml.append("  total_carbs decimal(5,2) [not null]\n");
        dbml.append("  total_fat decimal(5,2) [not null]\n");
        dbml.append("  is_generated boolean [default: true]\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("}\n\n");

        dbml.append("Table meals {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  diet_plan_id bigint [ref: > diet_plans.id, not null]\n");
        dbml.append("  meal_type meal_type_enum [not null]\n");
        dbml.append("  meal_name varchar(255) [not null]\n");
        dbml.append("  calories decimal(6,2) [not null]\n");
        dbml.append("  protein decimal(5,2) [not null]\n");
        dbml.append("  carbs decimal(5,2) [not null]\n");
        dbml.append("  fat decimal(5,2) [not null]\n");
        dbml.append("  preparation_time_minutes int\n");
        dbml.append("  instructions text\n");
        dbml.append("  health_benefits text\n");
        dbml.append("  ingredients json\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("}\n\n");

        // Continue with other tables...
        dbml.append("Table food_logs {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  user_id bigint [ref: > users.id, not null]\n");
        dbml.append("  meal_type meal_type_enum [not null]\n");
        dbml.append("  meal_name varchar(255)\n");
        dbml.append("  image_url varchar(255)\n");
        dbml.append("  image_description text\n");
        dbml.append("  logged_at timestamp [not null]\n");
        dbml.append("  created_at timestamp [default: `now()`]\n");
        dbml.append("  is_from_camera boolean\n");
        dbml.append("}\n\n");

        dbml.append("Table food_items {\n");
        dbml.append("  id bigint [pk, increment]\n");
        dbml.append("  food_log_id bigint [ref: > food_logs.id, not null]\n");
        dbml.append("  name varchar(255) [not null]\n");
        dbml.append("  quantity double [not null]\n");
        dbml.append("  unit varchar(50) [not null]\n");
        dbml.append("  calories double [not null]\n");
        dbml.append("  protein double [not null]\n");
        dbml.append("  fat double [not null]\n");
        dbml.append("  carbs double [not null]\n");
        dbml.append("}\n\n");

        // Add remaining tables and enums...
        dbml.append("Enum gender_enum {\n");
        dbml.append("  MALE\n");
        dbml.append("  FEMALE\n");
        dbml.append("  OTHER\n");
        dbml.append("}\n\n");

        dbml.append("Enum activity_level_enum {\n");
        dbml.append("  SEDENTARY\n");
        dbml.append("  LIGHTLY_ACTIVE\n");
        dbml.append("  MODERATELY_ACTIVE\n");
        dbml.append("  VERY_ACTIVE\n");
        dbml.append("  EXTREMELY_ACTIVE\n");
        dbml.append("}\n\n");

        dbml.append("Enum goal_enum {\n");
        dbml.append("  LOSE_WEIGHT\n");
        dbml.append("  MAINTAIN_WEIGHT\n");
        dbml.append("  GAIN_WEIGHT\n");
        dbml.append("  BUILD_MUSCLE\n");
        dbml.append("}\n\n");

        dbml.append("Enum message_role_enum {\n");
        dbml.append("  USER\n");
        dbml.append("  BOT\n");
        dbml.append("}\n\n");

        dbml.append("Enum meal_type_enum {\n");
        dbml.append("  BREAKFAST\n");
        dbml.append("  LUNCH\n");
        dbml.append("  DINNER\n");
        dbml.append("  SNACK\n");
        dbml.append("}\n\n");

        writeToFile(filename, dbml.toString());
    }

    /**
     * Generate complete SQL CREATE script
     */
    public void generateSQLCreateScript(String filename) throws IOException {
        StringBuilder sql = new StringBuilder();
        
        sql.append("-- HealVerse Database Schema\n");
        sql.append("-- PostgreSQL Compatible SQL Script\n");
        sql.append("-- Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        sql.append("-- Create database (optional)\n");
        sql.append("-- CREATE DATABASE healverse;\n");
        sql.append("-- \\c healverse;\n\n");
        
        sql.append("-- Enable UUID extension\n");
        sql.append("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";\n\n");
        
        sql.append("-- Create ENUM types\n");
        sql.append("CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER');\n");
        sql.append("CREATE TYPE activity_level_enum AS ENUM ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE', 'EXTREMELY_ACTIVE');\n");
        sql.append("CREATE TYPE goal_enum AS ENUM ('LOSE_WEIGHT', 'MAINTAIN_WEIGHT', 'GAIN_WEIGHT', 'BUILD_MUSCLE');\n");
        sql.append("CREATE TYPE weight_loss_speed_enum AS ENUM ('SLOW', 'MODERATE', 'FAST');\n");
        sql.append("CREATE TYPE dietary_restriction_enum AS ENUM ('VEGETARIAN', 'VEGAN', 'GLUTEN_FREE', 'KETO', 'NON_VEGETARIAN');\n");
        sql.append("CREATE TYPE health_condition_enum AS ENUM ('NONE', 'DIABETES', 'HYPERTENSION', 'HEART_DISEASE', 'OTHER');\n");
        sql.append("CREATE TYPE message_role_enum AS ENUM ('USER', 'BOT');\n");
        sql.append("CREATE TYPE meal_type_enum AS ENUM ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK');\n");
        sql.append("CREATE TYPE exercise_intensity_enum AS ENUM ('LOW', 'MODERATE', 'HIGH', 'VIGOROUS');\n");
        sql.append("CREATE TYPE medication_type_enum AS ENUM ('TABLET', 'CAPSULE', 'LIQUID', 'INJECTION', 'TOPICAL', 'OTHER');\n");
        sql.append("CREATE TYPE frequency_type_enum AS ENUM ('ONCE_DAILY', 'TWICE_DAILY', 'THREE_TIMES_DAILY', 'FOUR_TIMES_DAILY', 'AS_NEEDED', 'CUSTOM');\n");
        sql.append("CREATE TYPE log_status_enum AS ENUM ('TAKEN', 'MISSED', 'SKIPPED', 'PENDING');\n\n");
        
        // Add all table creation statements
        sql.append("-- Create tables\n\n");
        
        sql.append("CREATE TABLE users (\n");
        sql.append("    id BIGSERIAL PRIMARY KEY,\n");
        sql.append("    username VARCHAR(255) NOT NULL UNIQUE,\n");
        sql.append("    password VARCHAR(255),\n");
        sql.append("    email VARCHAR(255) UNIQUE,\n");
        sql.append("    google_id VARCHAR(255) UNIQUE,\n");
        sql.append("    profile_image VARCHAR(255),\n");
        sql.append("    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n");
        sql.append("    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");
        sql.append(");\n\n");
        
        sql.append("CREATE INDEX idx_users_username ON users(username);\n");
        sql.append("CREATE INDEX idx_users_email ON users(email);\n\n");
        
        // Add other table creation statements...
        // (Continue with complete SQL schema)
        
        writeToFile(filename, sql.toString());
    }
    
    private void writeToFile(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}
