package com.bytehealers.healverse.test;

import com.bytehealers.healverse.service.GamificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Quick test to verify GamificationService works without ID constraint violations
 */
@Component
public class GamificationTestRunner implements CommandLineRunner {

    @Autowired
    private GamificationService gamificationService;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Testing GamificationService...");
            
            // Test with a sample user ID
            Long testUserId = 1L;
            
            // This should work without throwing the ID constraint error
            gamificationService.recordDailyLogin(testUserId);
            
            System.out.println("✅ GamificationService test passed - no ID constraint violations!");
            
        } catch (Exception e) {
            System.err.println("❌ GamificationService test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
