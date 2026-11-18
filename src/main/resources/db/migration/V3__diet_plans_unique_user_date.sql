-- A user has at most one diet plan per day. Until now only the row lock taken while generating a
-- plan prevented duplicates; a stray duplicate made findByUserIdAndPlanDate throw for that day.

-- Keep the newest plan of each (user, day) and drop the older ones. Meals reference their plan,
-- so they go first. On a database without duplicates both statements delete nothing.
DELETE FROM meals
WHERE diet_plan_id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, plan_date ORDER BY created_at DESC NULLS LAST, id DESC) AS rn
        FROM diet_plans
    ) ranked
    WHERE rn > 1
);

DELETE FROM diet_plans
WHERE id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, plan_date ORDER BY created_at DESC NULLS LAST, id DESC) AS rn
        FROM diet_plans
    ) ranked
    WHERE rn > 1
);

ALTER TABLE diet_plans
    ADD CONSTRAINT uk_diet_plans_user_date UNIQUE (user_id, plan_date);
