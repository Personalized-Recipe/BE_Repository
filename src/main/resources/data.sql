-- 테스트 사용자 데이터
INSERT INTO users (username, password, name, age, gender, preferences, health_conditions, allergies, created_at, updated_at) 
VALUES ('testuser', 'password123', '테스트사용자', 25, 'M', '한식', '건강함', '없음', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 테스트 재료 데이터
INSERT INTO ingredient (name, weight_in_grams) 
VALUES ('김치', 500), ('돼지고기', 300), ('두부', 400), ('양파', 200), ('마늘', 50)
ON DUPLICATE KEY UPDATE name = name;

-- 테스트 냉장고 데이터
INSERT INTO refrigerator (user_id, name, description, created_at, updated_at)
SELECT u.id, '테스트냉장고', '테스트용 냉장고', NOW(), NOW()
FROM users u 
WHERE u.username = 'testuser'
ON DUPLICATE KEY UPDATE updated_at = NOW(); 