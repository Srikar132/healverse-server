package com.bytehealers.healverse.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ER diagram generator for HealVerse.
 * Tables, enums and relations are declared once and rendered to Mermaid, PlantUML, DBML and a standalone HTML viewer.
 * Keep in sync with the JPA entities in com.bytehealers.healverse.model.
 *
 * Usage: java ERDiagramGenerator.java [outputDir]   (default: docs/erd)
 */
public class ERDiagramGenerator {

    /** type: DB type; flags: any of PK, FK, UK, NN (not null). */
    record Col(String name, String type, String... flags) {
        boolean has(String f) {
            for (String x : flags) if (x.equals(f)) return true;
            return false;
        }
    }

    record Table(String name, String group, List<Col> cols) {}

    /** from (many side) -> to (one side). oneToOne marks a 1:1 relation. */
    record Rel(String from, String fromCol, String to, String toCol, boolean oneToOne, boolean enforced, String label) {}

    static final List<Table> TABLES = new ArrayList<>();
    static final List<Rel> RELS = new ArrayList<>();
    static final Map<String, List<String>> ENUMS = new LinkedHashMap<>();

    static Col c(String name, String type, String... flags) {
        return new Col(name, type, flags);
    }

    static void table(String name, String group, Col... cols) {
        TABLES.add(new Table(name, group, List.of(cols)));
    }

    static {
        // ---- enums ----
        ENUMS.put("gender", List.of("MALE", "FEMALE", "OTHER"));
        ENUMS.put("activity_level", List.of("SEDENTARY", "LIGHTLY_ACTIVE", "MODERATELY_ACTIVE", "VERY_ACTIVE", "EXTREMELY_ACTIVE"));
        ENUMS.put("goal", List.of("LOSE_WEIGHT", "MAINTAIN_WEIGHT", "GAIN_WEIGHT", "BUILD_MUSCLE", "IMPROVE_FITNESS"));
        ENUMS.put("weight_loss_speed", List.of("SLOW", "MODERATE", "FAST", "VERY_FAST"));
        ENUMS.put("dietary_restriction", List.of("NON_VEGETARIAN", "VEGETARIAN", "VEGAN", "PESCATARIAN", "KETO", "PALEO",
                "MEDITERRANEAN", "GLUTEN_FREE", "DAIRY_FREE", "LOW_CARB", "LOW_FAT"));
        ENUMS.put("health_condition", List.of("NONE", "DIABETES", "HYPERTENSION", "HEART_DISEASE", "THYROID", "PCOS",
                "ARTHRITIS", "DIGESTIVE_ISSUES", "ALLERGIES", "OTHER"));
        ENUMS.put("meal_type", List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK"));
        ENUMS.put("exercise_intensity", List.of("LOW", "MODERATE", "HIGH", "VERY_HIGH"));
        ENUMS.put("message_role", List.of("USER", "BOT"));

        // ---- core ----
        table("users", "core",
                c("id", "bigint", "PK"),
                c("username", "varchar", "UK", "NN"),
                c("password", "varchar"),
                c("email", "varchar", "UK"),
                c("google_id", "varchar", "UK"),
                c("profile_image", "varchar"),
                c("created_at", "timestamp"),
                c("updated_at", "timestamp"));

        table("user_profiles", "core",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("gender", "gender", "NN"),
                c("age", "int", "NN"),
                c("height_cm", "decimal(5,2)", "NN"),
                c("current_weight_kg", "decimal(5,2)", "NN"),
                c("target_weight_kg", "decimal(5,2)", "NN"),
                c("activity_level", "activity_level", "NN"),
                c("goal", "goal", "NN"),
                c("address", "varchar"),
                c("weight_loss_speed", "weight_loss_speed"),
                c("dietary_restriction", "dietary_restriction"),
                c("health_conditions", "health_condition"),
                c("other_health_condition_description", "varchar"),
                c("created_at", "timestamp"),
                c("updated_at", "timestamp"));

        // ---- chat ----
        table("conversations", "chat",
                c("id", "varchar", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("title", "varchar(500)"),
                c("created_at", "timestamp", "NN"),
                c("updated_at", "timestamp"));

        table("messages", "chat",
                c("id", "bigint", "PK"),
                c("conversation_id", "varchar", "FK", "NN"),
                c("content", "text", "NN"),
                c("role", "message_role", "NN"),
                c("created_at", "timestamp", "NN"));

        // ---- diet ----
        table("diet_plans", "diet",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("plan_date", "date", "NN"),
                c("total_calories", "decimal(6,2)", "NN"),
                c("total_protein", "decimal(5,2)", "NN"),
                c("total_carbs", "decimal(5,2)", "NN"),
                c("total_fat", "decimal(5,2)", "NN"),
                c("is_generated", "boolean"),
                c("created_at", "timestamp"));

        table("meals", "diet",
                c("id", "bigint", "PK"),
                c("diet_plan_id", "bigint", "FK", "NN"),
                c("meal_type", "meal_type", "NN"),
                c("meal_name", "varchar", "NN"),
                c("calories", "decimal(6,2)", "NN"),
                c("protein", "decimal(5,2)", "NN"),
                c("carbs", "decimal(5,2)", "NN"),
                c("fat", "decimal(5,2)", "NN"),
                c("preparation_time_minutes", "int"),
                c("instructions", "text"),
                c("health_benefits", "text"),
                c("ingredients", "json"),
                c("created_at", "timestamp"));

        // ---- logging ----
        table("food_logs", "logging",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK"),
                c("meal_type", "meal_type"),
                c("meal_name", "varchar"),
                c("image_url", "varchar"),
                c("image_description", "varchar"),
                c("logged_at", "timestamp"),
                c("created_at", "timestamp"),
                c("is_from_camera", "boolean"));

        table("food_items", "logging",
                c("id", "bigint", "PK"),
                c("food_log_id", "bigint", "FK"),
                c("name", "varchar"),
                c("quantity", "double"),
                c("unit", "varchar"),
                c("calories", "double"),
                c("protein", "double"),
                c("fat", "double"),
                c("carbs", "double"));

        table("exercise_logs", "logging",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("exercise_name", "varchar", "NN"),
                c("duration_minutes", "int", "NN"),
                c("intensity", "exercise_intensity", "NN"),
                c("calories_burned", "decimal(6,2)", "NN"),
                c("logged_at", "timestamp", "NN"),
                c("created_at", "timestamp"));

        table("water_logs", "logging",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("amount_ml", "decimal(8,2)", "NN"),
                c("logged_at", "timestamp", "NN"),
                c("created_at", "timestamp"));

        table("daily_nutrition_summaries", "logging",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("date", "date", "NN"),
                c("target_calories", "decimal(6,2)", "NN"),
                c("target_protein", "decimal(5,2)", "NN"),
                c("target_carbs", "decimal(5,2)", "NN"),
                c("target_fat", "decimal(5,2)", "NN"),
                c("consumed_calories", "decimal(6,2)"),
                c("consumed_protein", "decimal(5,2)"),
                c("consumed_carbs", "decimal(5,2)"),
                c("consumed_fat", "decimal(5,2)"),
                c("calories_burned", "decimal(6,2)"),
                c("water_consumed_ml", "decimal(8,2)"),
                c("target_water_ml", "decimal(8,2)"),
                c("remaining_calories", "decimal(6,2)"),
                c("created_at", "timestamp"),
                c("updated_at", "timestamp"));

        // ---- gamification (user_id is a plain column, no JPA relation / DB-level FK) ----
        table("user_streaks", "gamification",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "UK", "NN"),
                c("current_login_streak", "int"),
                c("longest_login_streak", "int"),
                c("last_login_date", "date"),
                c("total_points", "int"),
                c("created_at", "timestamp", "NN"),
                c("updated_at", "timestamp"));

        table("daily_points", "gamification",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("date", "date", "NN"),
                c("login_points", "int", "NN"),
                c("diet_points", "int", "NN"),
                c("total_points", "int", "NN"),
                c("created_at", "timestamp", "NN"));

        table("points_history", "gamification",
                c("id", "bigint", "PK"),
                c("user_id", "bigint", "FK", "NN"),
                c("points_earned", "int", "NN"),
                c("reason", "varchar(100)", "NN"),
                c("description", "text"),
                c("date", "date", "NN"),
                c("created_at", "timestamp", "NN"));

        // ---- relations (from = FK side) ----
        RELS.add(new Rel("user_profiles", "user_id", "users", "id", true, true, "has profile"));
        RELS.add(new Rel("conversations", "user_id", "users", "id", false, true, "has"));
        RELS.add(new Rel("messages", "conversation_id", "conversations", "id", false, true, "contains"));
        RELS.add(new Rel("diet_plans", "user_id", "users", "id", false, true, "has"));
        RELS.add(new Rel("meals", "diet_plan_id", "diet_plans", "id", false, true, "contains"));
        RELS.add(new Rel("food_logs", "user_id", "users", "id", false, true, "logs"));
        RELS.add(new Rel("food_items", "food_log_id", "food_logs", "id", false, true, "contains"));
        RELS.add(new Rel("exercise_logs", "user_id", "users", "id", false, true, "logs"));
        RELS.add(new Rel("water_logs", "user_id", "users", "id", false, true, "logs"));
        RELS.add(new Rel("daily_nutrition_summaries", "user_id", "users", "id", false, true, "has"));
        // logical only: gamification entities store userId without @ManyToOne
        RELS.add(new Rel("user_streaks", "user_id", "users", "id", true, false, "streak (logical)"));
        RELS.add(new Rel("daily_points", "user_id", "users", "id", false, false, "earns (logical)"));
        RELS.add(new Rel("points_history", "user_id", "users", "id", false, false, "earns (logical)"));
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/erd");
        Files.createDirectories(out);

        ERDiagramGenerator g = new ERDiagramGenerator();
        String mermaid = g.mermaid();
        Files.writeString(out.resolve("healverse-erd-mermaid.md"), "# HealVerse ER Diagram\n\n```mermaid\n" + mermaid + "```\n");
        Files.writeString(out.resolve("healverse-erd-plantuml.puml"), g.plantUml());
        Files.writeString(out.resolve("healverse-erd.dbml"), g.dbml());
        Files.writeString(out.resolve("healverse-erd.html"), g.html(mermaid));

        System.out.println("Generated in " + out.toAbsolutePath() + ":");
        System.out.println("  healverse-erd.html           open in browser (Mermaid, needs internet for CDN)");
        System.out.println("  healverse-erd-mermaid.md     GitHub / VS Code Mermaid preview");
        System.out.println("  healverse-erd-plantuml.puml  PlantUML");
        System.out.println("  healverse-erd.dbml           paste into dbdiagram.io");
    }

    static String mermaidType(String t) {
        // mermaid attribute types must be a single token
        return t.replaceAll("\\(.*\\)", "");
    }

    String mermaid() {
        StringBuilder sb = new StringBuilder("erDiagram\n");
        for (Table t : TABLES) {
            sb.append("    ").append(t.name()).append(" {\n");
            for (Col col : t.cols()) {
                sb.append("        ").append(mermaidType(col.type())).append(' ').append(col.name());
                List<String> keys = new ArrayList<>();
                for (String f : List.of("PK", "FK", "UK")) if (col.has(f)) keys.add(f);
                if (!keys.isEmpty()) sb.append(' ').append(String.join(",", keys));
                sb.append('\n');
            }
            sb.append("    }\n");
        }
        sb.append('\n');
        for (Rel r : RELS) {
            String left = r.oneToOne() ? "||" : "||";
            String right = r.oneToOne() ? "||" : "o{";
            // parent on the left, child on the right; dashed line (..) marks logical-only relations
            sb.append("    ").append(r.to()).append(' ').append(left).append(r.enforced() ? "--" : "..")
                    .append(right).append(' ').append(r.from()).append(" : \"").append(r.label()).append("\"\n");
        }
        return sb.toString();
    }

    String plantUml() {
        StringBuilder sb = new StringBuilder("@startuml HealVerse_ERD\n");
        sb.append("hide circle\nskinparam linetype ortho\n\n");
        for (Table t : TABLES) {
            sb.append("entity \"").append(t.name()).append("\" as ").append(t.name()).append(" {\n");
            for (Col col : t.cols()) {
                if (col.has("PK")) sb.append("  * ").append(col.name()).append(" : ").append(col.type()).append(" <<PK>>\n");
            }
            sb.append("  --\n");
            for (Col col : t.cols()) {
                if (col.has("PK")) continue;
                sb.append(col.has("NN") ? "  * " : "  ").append(col.name()).append(" : ").append(col.type());
                if (col.has("FK")) sb.append(" <<FK>>");
                if (col.has("UK")) sb.append(" <<UK>>");
                sb.append('\n');
            }
            sb.append("}\n\n");
        }
        for (Rel r : RELS) {
            sb.append(r.to()).append(" ||").append(r.enforced() ? "--" : "..")
                    .append(r.oneToOne() ? "||" : "o{").append(' ').append(r.from())
                    .append(" : ").append(r.label()).append('\n');
        }
        return sb.append("@enduml\n").toString();
    }

    String dbml() {
        StringBuilder sb = new StringBuilder();
        sb.append("// HealVerse schema - paste into https://dbdiagram.io/d\n\n");
        sb.append("Project HealVerse {\n  database_type: 'PostgreSQL'\n}\n\n");
        for (var e : ENUMS.entrySet()) {
            sb.append("Enum ").append(e.getKey()).append(" {\n");
            for (String v : e.getValue()) sb.append("  ").append(v).append('\n');
            sb.append("}\n\n");
        }
        for (Table t : TABLES) {
            sb.append("Table ").append(t.name()).append(" {\n");
            for (Col col : t.cols()) {
                List<String> attrs = new ArrayList<>();
                if (col.has("PK")) attrs.add("pk");
                if (col.has("UK")) attrs.add("unique");
                if (col.has("NN") && !col.has("PK")) attrs.add("not null");
                sb.append("  ").append(col.name()).append(' ').append(col.type());
                if (!attrs.isEmpty()) sb.append(" [").append(String.join(", ", attrs)).append(']');
                sb.append('\n');
            }
            sb.append("}\n\n");
        }
        for (Rel r : RELS) {
            sb.append("Ref: ").append(r.from()).append('.').append(r.fromCol())
                    .append(r.oneToOne() ? " - " : " > ").append(r.to()).append('.').append(r.toCol());
            if (!r.enforced()) sb.append(" // logical only, no JPA relation");
            sb.append('\n');
        }
        return sb.toString();
    }

    String html(String mermaid) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>HealVerse ER Diagram</title>
                <style>
                  body { margin: 0; padding: 24px; font-family: system-ui, sans-serif; background: #fff; color: #111; }
                  h1 { font-size: 20px; margin: 0 0 4px; }
                  p { margin: 0 0 16px; color: #555; font-size: 14px; }
                  .mermaid { overflow: auto; }
                </style>
                </head>
                <body>
                <h1>HealVerse database ER diagram</h1>
                <p>Solid lines = JPA relations. Dotted lines = logical only (gamification tables store user_id without a relation).</p>
                <pre class="mermaid">
                %s</pre>
                <script type="module">
                  import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
                  mermaid.initialize({ startOnLoad: true, maxTextSize: 200000, er: { useMaxWidth: false } });
                </script>
                </body>
                </html>
                """.formatted(mermaid);
    }
}
