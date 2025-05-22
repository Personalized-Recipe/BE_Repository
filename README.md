# 개인화된 레시피 추천 서비스

사용자의 건강 상태, 선호도, 제약사항에 맞는 맞춤형 레시피를 추천해주는 웹 애플리케이션입니다.

## 기능

- 사용자 정보 기반 맞춤형 레시피 추천
- 건강 상태, 알러지 정보를 고려한 안전한 레시피 제공
- 회원가입 없이도 기본 서비스 이용 가능
- 회원 전용 레시피 기록 저장 및 조회

## 기술 스택

### 백엔드
- Spring Boot
- Spring Security + JWT
- Spring Data JPA
- H2 Database (개발용)
- Perplexity API 연동

### 프론트엔드
- React
- React Router
- Axios

## 설치 및 실행 방법

### 백엔드 실행
```bash
# 프로젝트 루트 디렉토리에서
./gradlew bootRun
```

### 프론트엔드 실행
```bash
# frontend 디렉토리에서
npm install
npm start
```

## 환경 설정

### application.properties 설정
```properties
# Perplexity API 키 설정
perplexity.api.key=your-perplexity-api-key
perplexity.api.url=https://api.perplexity.ai/chat/completions

# JWT 설정
jwt.secret=your-jwt-secret-key
jwt.expiration=86400000
```

## 프로젝트 구조

### 백엔드
- `entity`: 데이터베이스 모델 클래스
- `repository`: 데이터 액세스 인터페이스
- `service`: 비즈니스 로직 클래스
- `controller`: API 엔드포인트 클래스
- `dto`: 데이터 전송 객체 클래스
- `config`: 설정 클래스

### 프론트엔드
- `components`: 재사용 가능한 UI 컴포넌트
- `pages`: 페이지 컴포넌트
- `services`: API 통신 서비스 