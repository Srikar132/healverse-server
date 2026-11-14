package com.bytehealers.healverse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Opt-in check against the real Groq API (costs a few cents). It is skipped unless GROQ_API_KEY is set:
 *
 * <pre>GROQ_API_KEY=gsk_... ./mvnw test -Dtest=GroqLiveSmokeTests</pre>
 *
 * Everything else (database, users) is in-memory, so it never touches a real database.
 */
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
@SpringBootTest(properties = {
        "spring.ai.openai.base-url=https://api.groq.com/openai",
        "spring.ai.openai.api-key=${GROQ_API_KEY}",
        "spring.ai.openai.chat.options.model=openai/gpt-oss-120b",
        "app.ai.vision-model=qwen/qwen3.8-27b"
})
@AutoConfigureMockMvc
class GroqLiveSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    /** Groq's on-demand tier allows few tokens per minute; spacing the tests keeps them from tripping HTTP 429. */
    @BeforeEach
    void respectTokensPerMinuteLimit() throws InterruptedException {
        Thread.sleep(25_000);
    }

    @Autowired
    private ObjectMapper objectMapper;

    private String token() throws Exception {
        Map<String, Object> body = Map.of(
                "user", Map.of("username", "groq" + UUID.randomUUID().toString().substring(0, 8), "password", "Passw0rd!x"),
                "profile", Map.of("gender", "MALE", "age", 30, "heightCm", 175, "currentWeightKg", 80,
                        "targetWeightKg", 75, "activityLevel", "LIGHTLY_ACTIVE", "goal", "LOSE_WEIGHT",
                        "healthCondition", "NONE"));
        String json = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(json).get("data").get("token").asText();
    }

    private JsonNode data(MvcResult result) throws Exception {
        assertTrue(result.getResponse().getStatus() == 200,
                "HTTP " + result.getResponse().getStatus() + ": " + result.getResponse().getContentAsString());
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    @Test
    void dietPlanIsGeneratedAndParsedFromTheTextModel() throws Exception {
        String auth = token();
        JsonNode plan = data(mockMvc.perform(post("/api/diet-plans/generate/" + LocalDate.now())
                .header("Authorization", auth)).andReturn());

        JsonNode meals = plan.get("meals");
        assertTrue(meals.size() >= 3, "expected at least breakfast, lunch and dinner but got " + meals.size());
        for (JsonNode meal : meals) {
            assertTrue(meal.get("calories").asDouble() > 0, "meal without calories: " + meal.get("mealName"));
            assertFalse(meal.get("mealName").asText().isBlank());
        }
        System.out.println("[groq] diet plan: " + meals.size() + " meals, " + plan.get("totalCalories") + " kcal");
    }

    @Test
    void foodDescriptionIsAnalysedByTheTextModel() throws Exception {
        String auth = token();
        JsonNode log = data(mockMvc.perform(post("/api/food-logs/log/description")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"BREAKFAST\",\"description\":\"two boiled eggs and a slice of toast\"}"))
                .andReturn());

        assertTrue(log.get("items").size() >= 1, "no food items parsed");
        System.out.println("[groq] food description: " + log.get("mealName") + ", " + log.get("items").size() + " items");
    }

    @Test
    void foodPhotoIsAnalysedByTheVisionModel() throws Exception {
        String auth = token();
        // A drawn apple stands in for a photo: this proves the model id, image format and JSON round trip work.
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 245, 235));
        g.fillRect(0, 0, 512, 512);
        g.setColor(new Color(200, 30, 30));
        g.fillOval(96, 140, 320, 300);
        g.setColor(new Color(90, 60, 30));
        g.fillRect(250, 90, 14, 70);
        g.setColor(new Color(60, 150, 60));
        g.fillOval(264, 80, 90, 40);
        g.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);

        JsonNode log = data(mockMvc.perform(multipart("/api/food-logs/log/image")
                        .file(new MockMultipartFile("image", "apple.png", "image/png", png.toByteArray()))
                        .file(new MockMultipartFile("mealType", "", "text/plain", "SNACK".getBytes()))
                        .header("Authorization", auth))
                .andReturn());

        assertTrue(log.get("isFromCamera").asBoolean());
        System.out.println("[groq] food photo: " + log.get("mealName") + ", " + log.get("items").size() + " items");
    }

    @Test
    void chatRepliesThroughTheTextModel() throws Exception {
        String auth = token();
        JsonNode reply = data(mockMvc.perform(post("/api/conversations/" + UUID.randomUUID() + "/messages")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Give me one tip for eating more protein.\",\"sender\":\"user\"}"))
                .andReturn());

        assertFalse(reply.toString().isBlank());
        System.out.println("[groq] chat reply received (" + reply.toString().length() + " chars)");
    }
}
