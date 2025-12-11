# HealVerse Streak Points & Rewards System Plan

## Overview
A gamification system that rewards users for consistently following AI-generated diet plans and maintaining healthy habits, encouraging long-term engagement and better health outcomes.

## 1. Database Schema Design

### New Tables Required

#### 1.1 User Points & Streaks
```sql
-- Main points and streak tracking
CREATE TABLE user_streaks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    streak_type ENUM('DIET_PLAN', 'WATER_INTAKE', 'EXERCISE', 'MEDICATION', 'OVERALL') NOT NULL,
    current_streak INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_activity_date DATE,
    streak_start_date DATE,
    total_points INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_streak_type (user_id, streak_type)
);

-- Daily points tracking
CREATE TABLE daily_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    date DATE NOT NULL,
    diet_plan_points INT DEFAULT 0,
    water_intake_points INT DEFAULT 0,
    exercise_points INT DEFAULT 0,
    medication_points INT DEFAULT 0,
    bonus_points INT DEFAULT 0,
    total_daily_points INT DEFAULT 0,
    achievements_unlocked JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_date (user_id, date)
);

-- Points transaction log
CREATE TABLE points_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    transaction_type ENUM('EARNED', 'REDEEMED', 'BONUS', 'PENALTY') NOT NULL,
    points_amount INT NOT NULL,
    source_type ENUM('DIET_ADHERENCE', 'WATER_INTAKE', 'EXERCISE', 'MEDICATION', 'STREAK_BONUS', 'ACHIEVEMENT', 'REWARD_REDEMPTION') NOT NULL,
    source_id BIGINT, -- Reference to specific activity (diet_plan_id, exercise_log_id, etc.)
    description TEXT,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 1.2 Rewards & Achievements System
```sql
-- Reward definitions
CREATE TABLE rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    reward_type ENUM('BADGE', 'POINTS_MULTIPLIER', 'DISCOUNT', 'PREMIUM_FEATURE', 'VIRTUAL_GIFT') NOT NULL,
    points_required INT NOT NULL,
    reward_value JSON, -- Store reward-specific data
    is_active BOOLEAN DEFAULT TRUE,
    validity_days INT, -- How long reward is valid after earning
    max_redemptions INT, -- Per user limit
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User earned rewards
CREATE TABLE user_rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    reward_id BIGINT NOT NULL REFERENCES rewards(id),
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    redeemed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    is_redeemed BOOLEAN DEFAULT FALSE,
    redemption_data JSON -- Store redemption-specific data
);

-- Achievement definitions
CREATE TABLE achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    achievement_type ENUM('STREAK', 'POINTS', 'CONSISTENCY', 'MILESTONE', 'SPECIAL') NOT NULL,
    criteria JSON, -- Store achievement criteria (e.g., {"streak_days": 7, "activity_type": "DIET_PLAN"})
    points_reward INT DEFAULT 0,
    badge_image_url VARCHAR(255),
    rarity ENUM('COMMON', 'RARE', 'EPIC', 'LEGENDARY') DEFAULT 'COMMON',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User earned achievements
CREATE TABLE user_achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    achievement_id BIGINT NOT NULL REFERENCES achievements(id),
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    progress_data JSON, -- Track partial progress
    UNIQUE KEY unique_user_achievement (user_id, achievement_id)
);
```

#### 1.3 Diet Plan Adherence Tracking
```sql
-- Track how well user follows AI-suggested diet plans
CREATE TABLE diet_plan_adherence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    diet_plan_id BIGINT NOT NULL REFERENCES diet_plans(id),
    date DATE NOT NULL,
    meals_followed INT DEFAULT 0, -- How many suggested meals were actually consumed
    total_meals INT DEFAULT 0, -- Total meals in the plan (usually 3)
    adherence_percentage DECIMAL(5,2) DEFAULT 0, -- 0-100%
    calories_adherence DECIMAL(5,2) DEFAULT 0, -- How close to target calories
    macro_adherence_score DECIMAL(5,2) DEFAULT 0, -- Combined protein/carb/fat adherence
    points_earned INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_date_plan (user_id, date, diet_plan_id)
);

-- Detailed meal adherence
CREATE TABLE meal_adherence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    diet_plan_adherence_id BIGINT NOT NULL REFERENCES diet_plan_adherence(id),
    suggested_meal_id BIGINT REFERENCES meals(id),
    actual_food_log_id BIGINT REFERENCES food_logs(id),
    meal_type ENUM('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK') NOT NULL,
    adherence_score DECIMAL(5,2) DEFAULT 0, -- 0-100% how similar actual meal to suggested
    points_earned INT DEFAULT 0,
    notes TEXT
);
```

## 2. Points & Streaks Calculation Logic

### 2.1 Diet Plan Adherence Scoring

#### Base Points System:
```
Perfect Meal Adherence (90-100%): 50 points
Good Adherence (70-89%): 35 points  
Fair Adherence (50-69%): 20 points
Poor Adherence (30-49%): 10 points
No Adherence (0-29%): 0 points

Daily Bonuses:
- All 3 meals followed: +25 bonus points
- Perfect calorie adherence (±5%): +15 points
- Perfect macro balance (±10%): +10 points
```

#### Adherence Calculation Methods:
```
Meal Similarity Score:
- Ingredient match: 40%
- Calorie similarity: 30%  
- Macro distribution: 30%

Daily Adherence Score:
- Average of all meal adherence scores
- Weighted by meal importance (dinner 40%, lunch 35%, breakfast 25%)
```

### 2.2 Streak Calculation

#### Streak Types:
1. **Diet Plan Streak**: Consecutive days following AI diet plans (≥70% adherence)
2. **Water Intake Streak**: Meeting daily water targets
3. **Exercise Streak**: Logging any exercise activity
4. **Medication Streak**: Taking medications on time
5. **Overall Health Streak**: Meeting 3/4 of above criteria daily

#### Streak Bonuses:
```
Diet Plan Streaks:
- 3 days: +50 bonus points
- 7 days: +150 bonus points  
- 14 days: +350 bonus points
- 30 days: +800 bonus points
- 60 days: +1500 bonus points
- 100+ days: +100 points per additional day

Multipliers:
- 7+ day streak: 1.2x daily points
- 14+ day streak: 1.4x daily points
- 30+ day streak: 1.6x daily points
- 60+ day streak: 1.8x daily points
- 100+ day streak: 2x daily points
```

## 3. Achievement System

### 3.1 Achievement Categories

#### Streak Achievements:
```json
[
  {
    "name": "Diet Novice",
    "description": "Follow AI diet plan for 3 consecutive days",
    "criteria": {"streak_days": 3, "activity_type": "DIET_PLAN"},
    "points_reward": 100,
    "rarity": "COMMON"
  },
  {
    "name": "Nutrition Enthusiast", 
    "description": "Follow AI diet plan for 7 consecutive days",
    "criteria": {"streak_days": 7, "activity_type": "DIET_PLAN"},
    "points_reward": 300,
    "rarity": "RARE"
  },
  {
    "name": "Diet Champion",
    "description": "Follow AI diet plan for 30 consecutive days", 
    "criteria": {"streak_days": 30, "activity_type": "DIET_PLAN"},
    "points_reward": 1000,
    "rarity": "EPIC"
  }
]
```

#### Performance Achievements:
```json
[
  {
    "name": "Perfect Week",
    "description": "Achieve 100% diet adherence for 7 days",
    "criteria": {"perfect_days": 7, "adherence_threshold": 100},
    "points_reward": 500,
    "rarity": "RARE"
  },
  {
    "name": "Calorie Master",
    "description": "Stay within ±50 calories of target for 14 days",
    "criteria": {"calorie_accuracy_days": 14, "variance_threshold": 50},
    "points_reward": 400,
    "rarity": "RARE"
  }
]
```

#### Milestone Achievements:
```json
[
  {
    "name": "Point Collector",
    "description": "Earn 1,000 total points",
    "criteria": {"total_points": 1000},
    "points_reward": 200,
    "rarity": "COMMON"
  },
  {
    "name": "Health Guru", 
    "description": "Earn 10,000 total points",
    "criteria": {"total_points": 10000},
    "points_reward": 1000,
    "rarity": "LEGENDARY"
  }
]
```

## 4. Rewards System

### 4.1 Reward Categories

#### Virtual Badges & Status:
```json
[
  {
    "name": "Nutrition Expert Badge",
    "points_required": 2000,
    "reward_type": "BADGE",
    "reward_value": {"badge_id": "nutrition_expert", "display_name": "🥗 Nutrition Expert"}
  },
  {
    "name": "Health Champion Status",
    "points_required": 5000,
    "reward_type": "BADGE", 
    "reward_value": {"status_level": "champion", "profile_border": "gold"}
  }
]
```

#### Feature Unlocks:
```json
[
  {
    "name": "Premium Recipe Collection",
    "points_required": 1500,
    "reward_type": "PREMIUM_FEATURE",
    "reward_value": {"feature_id": "premium_recipes", "duration_days": 30}
  },
  {
    "name": "Advanced Analytics",
    "points_required": 3000,
    "reward_type": "PREMIUM_FEATURE",
    "reward_value": {"feature_id": "advanced_analytics", "duration_days": 30}
  }
]
```

#### Points Multipliers:
```json
[
  {
    "name": "2x Points Booster (24h)",
    "points_required": 800,
    "reward_type": "POINTS_MULTIPLIER",
    "reward_value": {"multiplier": 2.0, "duration_hours": 24}
  },
  {
    "name": "1.5x Points Booster (7 days)",
    "points_required": 2000,
    "reward_type": "POINTS_MULTIPLIER", 
    "reward_value": {"multiplier": 1.5, "duration_days": 7}
  }
]
```

#### Virtual Gifts & Customization:
```json
[
  {
    "name": "Custom Avatar Outfit",
    "points_required": 1200,
    "reward_type": "VIRTUAL_GIFT",
    "reward_value": {"item_type": "avatar_outfit", "item_id": "chef_uniform"}
  },
  {
    "name": "Animated Profile Theme",
    "points_required": 1800,
    "reward_type": "VIRTUAL_GIFT",
    "reward_value": {"item_type": "profile_theme", "theme_id": "nature_zen"}
  }
]
```

## 5. Service Architecture

### 5.1 Core Services

#### StreakService
```java
@Service
public class StreakService {
    // Calculate and update user streaks
    public void updateDailyStreak(Long userId, StreakType streakType, boolean activityCompleted);
    public UserStreakSummary getUserStreaks(Long userId);
    public List<StreakMilestone> getUpcomingMilestones(Long userId);
    public void handleStreakBreak(Long userId, StreakType streakType);
    public Map<StreakType, Integer> calculateWeeklyStreakSummary(Long userId);
}
```

#### PointsService  
```java
@Service
public class PointsService {
    // Points calculation and management
    public int calculateDietAdherencePoints(DietPlanAdherence adherence);
    public void awardPoints(Long userId, PointsTransaction transaction);
    public UserPointsSummary getUserPointsSummary(Long userId);
    public List<PointsTransaction> getPointsHistory(Long userId, LocalDate fromDate);
    public boolean redeemReward(Long userId, Long rewardId);
}
```

#### AdherenceTrackingService
```java
@Service  
public class AdherenceTrackingService {
    // Diet plan adherence calculation
    public DietPlanAdherence calculateDailyAdherence(Long userId, LocalDate date);
    public MealAdherence calculateMealSimilarity(Meal suggestedMeal, FoodLog actualMeal);
    public double calculateIngredientSimilarity(List<String> suggested, List<FoodItem> actual);
    public AdherenceReport generateWeeklyReport(Long userId);
}
```

#### AchievementService
```java
@Service
public class AchievementService {
    // Achievement processing
    public List<Achievement> checkForNewAchievements(Long userId);
    public void awardAchievement(Long userId, Long achievementId);
    public List<UserAchievement> getUserAchievements(Long userId);
    public AchievementProgress getAchievementProgress(Long userId, Long achievementId);
}
```

#### RewardService
```java
@Service
public class RewardService {
    // Reward management
    public List<Reward> getAvailableRewards(Long userId);
    public UserReward redeemReward(Long userId, Long rewardId);
    public List<UserReward> getUserRewards(Long userId);
    public boolean isRewardEligible(Long userId, Long rewardId);
    public void processExpiredRewards();
}
```

### 5.2 Manager Service (Orchestration)

#### GamificationManagerService
```java
@Service
public class GamificationManagerService {
    
    @Autowired
    private StreakService streakService;
    
    @Autowired  
    private PointsService pointsService;
    
    @Autowired
    private AdherenceTrackingService adherenceService;
    
    @Autowired
    private AchievementService achievementService;
    
    @Autowired
    private RewardService rewardService;
    
    // Main orchestration methods
    public DailyGamificationUpdate processDailyActivity(Long userId, LocalDate date);
    public GamificationDashboard getUserDashboard(Long userId);
    public WeeklyGamificationReport generateWeeklyReport(Long userId);
    public void processNightlyUpdates(); // Scheduled task
    
    // Event-driven processing
    public void onFoodLogCreated(FoodLog foodLog);
    public void onExerciseLogged(ExerciseLog exerciseLog);  
    public void onWaterLogged(WaterLog waterLog);
    public void onMedicationTaken(MedicationLog medicationLog);
}
```

## 6. API Endpoints Structure

### 6.1 Points & Streaks APIs
```
GET /api/gamification/dashboard/{userId}
GET /api/gamification/streaks/{userId}
GET /api/gamification/points/{userId}
GET /api/gamification/points/{userId}/history
POST /api/gamification/calculate-adherence/{userId}/{date}
```

### 6.2 Achievements APIs  
```
GET /api/gamification/achievements/{userId}
GET /api/gamification/achievements/available
GET /api/gamification/achievements/{userId}/progress
```

### 6.3 Rewards APIs
```
GET /api/gamification/rewards/available/{userId}  
GET /api/gamification/rewards/{userId}/earned
POST /api/gamification/rewards/{rewardId}/redeem
GET /api/gamification/rewards/{userId}/history
```

## 7. Integration Points

### 7.1 Existing Service Integration

#### With DietPlanService:
- Track when users generate new diet plans
- Monitor plan adherence vs actual food logs
- Calculate meal similarity scores

#### With FoodLoggingService:  
- Trigger adherence calculation when food is logged
- Compare logged food with AI-suggested meals
- Update daily points based on adherence

#### With NutritionSyncService:
- Use daily nutrition summaries for adherence calculations
- Track calorie and macro adherence accuracy

#### With ChatService:
- Provide gamification context in AI conversations
- Allow users to ask about their streaks/points
- Motivational messages based on performance

## 8. Implementation Phases

### Phase 1: Core Infrastructure (2-3 weeks)
- Database schema creation
- Basic services (StreakService, PointsService)
- Adherence calculation logic
- Manager service foundation

### Phase 2: Gamification Logic (2-3 weeks)  
- Achievement system implementation
- Reward system basics
- Daily processing workflows
- API endpoints

### Phase 3: Advanced Features (2-3 weeks)
- Complex achievements
- Premium rewards
- Analytics dashboard
- Performance optimizations

### Phase 4: Polish & Integration (1-2 weeks)
- UI integration points
- Testing & bug fixes
- Performance monitoring
- Documentation

## 9. Key Success Metrics

### User Engagement:
- Daily active users increase
- Average session duration
- Diet plan adherence rates
- Feature usage analytics

### Gamification Effectiveness:
- Points earned distribution
- Achievement unlock rates  
- Reward redemption patterns
- Streak length analysis

### Health Outcomes:
- Improved diet plan following
- Better nutrition goal achievement
- Increased app retention
- User satisfaction scores

This comprehensive plan provides a robust foundation for implementing a streak points and rewards system that will significantly enhance user engagement while promoting better health outcomes through AI-guided diet plan adherence.
