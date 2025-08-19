-- 사용자 계정 정보를 저장하는 테이블
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NULL, -- 소셜 로그인은 비밀번호가 없음
  `name` VARCHAR(255) NOT NULL,
  `provider` VARCHAR(50) NOT NULL, -- 예: GOOGLE, LOCAL
  `provider_id` VARCHAR(255) NULL, -- 소셜 로그인 제공자의 사용자 고유 ID
  `role` VARCHAR(50) NOT NULL, -- 예: USER, ADMIN
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_email` (`email` ASC) -- 이메일은 고유해야 함
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 프로필 정보를 저장하는 테이블
CREATE TABLE `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `bio` TEXT NULL,
  `location` VARCHAR(255) NULL,
  `website` VARCHAR(255) NULL,
  `phone_number` VARCHAR(50) NULL,
  `avatar_url` VARCHAR(500) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_id` (`user_id` ASC), -- 한 명의 유저는 하나의 프로필만 가짐
  -- users 테이블의 id를 참조하는 외래키. 사용자가 삭제되면 프로필도 함께 삭제됨 (CASCADE)
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;