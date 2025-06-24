CREATE TABLE Recipe (
    recipe_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL COMMENT '레시피 이름',
    description TEXT COMMENT '레시피 설명',
    category VARCHAR(50) COMMENT '레시피 카테고리(한식,중식,일식,양식,분식,퓨전)' CHECK (category IN ('한식', '중식', '일식', '양식', '분식', '퓨전')),
    image BLOB COMMENT '레시피 이미지',
    cooking_time INT COMMENT '분 단위',
    difficulty VARCHAR(5) COMMENT '레시피 난이도(상,중,하)' CHECK (difficulty IN ('상', '중', '하')),
    PRIMARY KEY (recipe_id)
);


CREATE TABLE User (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '사용자 이름',
    nickname VARCHAR(50) COMMENT '사용자 입력 이름',
    password VARCHAR(100) NOT NULL COMMENT '비밀번호',
    provider VARCHAR(20) COMMENT '소셜 플랫폼(KAKAO, GOOGLE, NAVER 등)',
    provider_id VARCHAR(50) COMMENT '소셜 플랫폼 고유 ID',
    email VARCHAR(100) COMMENT '소셜에서 받아온 이메일',
    profile_image VARCHAR(255) COMMENT '프로필 이미지 URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정일',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_provider_provider_id (provider, provider_id) COMMENT '같은 플랫폼에서 같은 유저가 중복 가입되는걸 방지'
);

-- 유저 권한 테이블 및 샘플 데이터
CREATE TABLE user_roles (
    user_user_id INT NOT NULL,
    roles VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_user_id) REFERENCES User(user_id)
);
-- 레시피에 필요한 재료 정보 테이블
CREATE TABLE Ingredient (
    ingredient_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '재료 이름',
    required_amount FLOAT COMMENT 'g 또는 ml단위',
    calories INT COMMENT '100g 당 칼로리',
    nutrition_info TEXT COMMENT '영양 정보 (단백질, 지방, 탄수화물 등)',
    creator_user_id INT NOT NULL COMMENT '재료를 등록한 사용자 ID',
    PRIMARY KEY (ingredient_id),
    FOREIGN KEY (creator_user_id) REFERENCES User(user_id),
    UNIQUE KEY uk_name_creator (name, creator_user_id) COMMENT '같은 사용자가 같은 이름의 재료를 중복 등록하지 못하도록 제약'
);

-- 사용자의 프롬프트 관련 정보를 저장하는 테이블
CREATE TABLE user_prompt (
    prompt_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NULL COMMENT 'User와 1:1 관계이며 초기에는 NULL 허용(사용자가 만들어짐과 동시에 프롬프트가 생겨야하는게 아니니까.)',
    name VARCHAR(10) NOT NULL COMMENT '이름은 필수 입력',
    age INT NULL CHECK (age > 0) COMMENT '나이는 양수만 허용',
    gender VARCHAR(2) NULL CHECK (gender IN ('M', 'F')) COMMENT '성별은 M 또는 F만 허용',
    is_pregnant BOOLEAN NULL COMMENT '임신 여부 선택 입력',
    health_status VARCHAR(50) NULL COMMENT '건강 상태 선택 입력',
    allergy VARCHAR(50) NULL COMMENT '알레르기 선택 입력',
    preference VARCHAR(100) NULL COMMENT '선호도 선택 입력',
    nickname VARCHAR(10) NULL COMMENT '닉네임 선택 입력',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '정보 입력일',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정일',
    PRIMARY KEY (prompt_id),
    FOREIGN KEY (user_id) REFERENCES User(user_id),
    UNIQUE (user_id) COMMENT 'User와 1:1 관계 보장'
);

-- 성별이 'M'일 경우 is_pregnant를 자동으로 false로 설정하는 트리거
DELIMITER //
CREATE TRIGGER set_pregnancy_for_male
BEFORE INSERT ON user_prompt
FOR EACH ROW
BEGIN
    IF NEW.gender = 'M' THEN
        SET NEW.is_pregnant = false;
    END IF;
END;//

CREATE TRIGGER update_pregnancy_for_male
BEFORE UPDATE ON user_prompt
FOR EACH ROW
BEGIN
    IF NEW.gender = 'M' THEN
        SET NEW.is_pregnant = false;
    END IF;
END;//

-- ★ UserPrompt.name → User.nickname 자동 동기화 트리거
CREATE TRIGGER sync_nickname_after_userprompt_insert
AFTER INSERT ON user_prompt
FOR EACH ROW
BEGIN
    UPDATE User
    SET nickname = NEW.name
    WHERE user_id = NEW.user_id;
END;//

CREATE TRIGGER sync_nickname_after_userprompt_update
AFTER UPDATE ON user_prompt
FOR EACH ROW
BEGIN
    UPDATE User
    SET nickname = NEW.name
    WHERE user_id = NEW.user_id;
END;//
DELIMITER ;

CREATE TABLE User_Ingredient (
    ingredient_id INT NOT NULL AUTO_INCREMENT COMMENT '재료 ID',
    weight_in_grams FLOAT CHECK (weight_in_grams > 0) COMMENT '보유한 재료의 양(g or ml 변수명은 grams 이긴함..)',
    user_id INT NOT NULL COMMENT '소유 사용자 ID',
    PRIMARY KEY (ingredient_id),
    FOREIGN KEY (user_id) REFERENCES User(user_id)
);

-- 레시피-재료 관계 테이블 삭제

CREATE TABLE User_Recipe (
    user_id INT NOT NULL,
    recipe_id INT NOT NULL,
    PRIMARY KEY (user_id, recipe_id),
    FOREIGN KEY (user_id) REFERENCES User(user_id),
    FOREIGN KEY (recipe_id) REFERENCES Recipe(recipe_id)
);

CREATE TABLE Chat_History (
    chat_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '사용자 ID',
    message TEXT NOT NULL COMMENT '사용자 메시지 또는 시스템 응답',
    is_user_message BOOLEAN NOT NULL COMMENT '사용자 메시지 여부(사용자가 입력한 메시지 인지 아니면 시스템 응답인지 구분)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '메시지 생성 시간',
    session_id VARCHAR(50) NOT NULL COMMENT '대화 세션 구분을 위한 ID',
    recipe_id INT NULL COMMENT '추천된 레시피가 있는 경우 연결',
    PRIMARY KEY (chat_id),
    FOREIGN KEY (user_id) REFERENCES User(user_id),
    FOREIGN KEY (recipe_id) REFERENCES Recipe(recipe_id),
    INDEX idx_user_session (user_id, session_id) COMMENT '사용자별 세션 조회를 위한 인덱스',
    INDEX idx_created_at (created_at) COMMENT '오래된 데이터 아카이빙을 위한 인덱스'
);