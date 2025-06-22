# 🍳 완전한 API 사용법 가이드

## 📋 목차
1. [헬스 체크 API](#헬스-체크-api)
2. [사용자 관리 API](#사용자-관리-api)
3. [재료 관리 API](#재료-관리-api)
4. [냉장고 관리 API](#냉장고-관리-api)
5. [냉장고 재료 관리 API](#냉장고-재료-관리-api)
6. [레시피 추천 API](#레시피-추천-api)
7. [사용자 프롬프트 API](#사용자-프롬프트-api)
8. [채팅 API](#채팅-api)
9. [검색 API](#검색-api)
10. [DTO 구조](#dto-구조)

---

## 🏥 헬스 체크 API

### GET `/api/health`
**설명**: 서버 상태 확인
```bash
curl -X GET http://localhost:8080/api/health
```
**응답**: `"OK"`

---

## 👤 사용자 관리 API

### GET `/api/users`
**설명**: 모든 사용자 조회
```bash
curl -X GET http://localhost:8080/api/users
```

### GET `/api/users/{userId}`
**설명**: 특정 사용자 프로필 조회
```bash
curl -X GET http://localhost:8080/api/users/1
```

### POST `/api/users/register`
**설명**: 사용자 등록
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "name": "테스트 사용자",
    "age": 25,
    "gender": "남성",
    "preferences": "한식",
    "healthConditions": "없음",
    "allergies": "없음"
  }'
```

**UserDTO 구조**:
```json
{
  "username": "string",
  "password": "string", 
  "name": "string",
  "age": "number",
  "gender": "string",
  "preferences": "string",
  "healthConditions": "string",
  "allergies": "string"
}
```

---

## 🥕 재료 관리 API

### GET `/api/ingredients`
**설명**: 모든 재료 조회
```bash
curl -X GET http://localhost:8080/api/ingredients
```

### GET `/api/ingredients/{ingredientId}`
**설명**: 특정 재료 조회
```bash
curl -X GET http://localhost:8080/api/ingredients/1
```

### POST `/api/ingredients`
**설명**: 재료 생성
```bash
curl -X POST http://localhost:8080/api/ingredients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "양파",
    "calories": 40,
    "nutritionInfo": "비타민C, 식이섬유",
    "requiredAmount": 100.0
  }'
```

### GET `/api/ingredients/search?query={검색어}`
**설명**: 재료 검색 (URL 인코딩 필요)
```bash
# 한글 검색어는 URL 인코딩 필요
curl -X GET "http://localhost:8080/api/ingredients/search?query=%EC%96%91%ED%8C%8C"
```

**Ingredient 엔티티 구조**:
```json
{
  "ingredientId": "number",
  "name": "string",
  "calories": "number",
  "nutritionInfo": "string",
  "requiredAmount": "number"
}
```

---

## 🏠 냉장고 관리 API

### GET `/api/refrigerators`
**설명**: 모든 냉장고 조회
```bash
curl -X GET http://localhost:8080/api/refrigerators
```

### GET `/api/refrigerators/{refrigeratorId}`
**설명**: 특정 냉장고 조회
```bash
curl -X GET http://localhost:8080/api/refrigerators/1
```

### POST `/api/refrigerators`
**설명**: 냉장고 생성
```bash
curl -X POST http://localhost:8080/api/refrigerators \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 냉장고",
    "userId": 1
  }'
```

**RefrigeratorDTO 구조**:
```json
{
  "refrigeratorId": "number",
  "name": "string",
  "userId": "number"
}
```

---

## 🥬 냉장고 재료 관리 API

### GET `/api/refrigerators/{refrigeratorId}/ingredients`
**설명**: 냉장고의 모든 재료 조회
```bash
curl -X GET http://localhost:8080/api/refrigerators/1/ingredients
```

### POST `/api/refrigerators/{refrigeratorId}/ingredients`
**설명**: 냉장고에 재료 추가
```bash
curl -X POST http://localhost:8080/api/refrigerators/1/ingredients \
  -H "Content-Type: application/json" \
  -d '{
    "ingredientId": 1,
    "quantity": 2.5,
    "unit": "개",
    "purchaseDate": "2024-12-20",
    "expiryDate": "2024-12-31",
    "storageLocation": "냉장실",
    "notes": "양파 2.5개"
  }'
```

### GET `/api/refrigerators/{refrigeratorId}/ingredients/search?q={검색어}`
**설명**: 냉장고 내 재료 검색 (URL 인코딩 필요)
```bash
curl -X GET "http://localhost:8080/api/refrigerators/1/ingredients/search?q=%EC%96%91%ED%8C%8C"
```

**RefrigeratorIngredientRequestDTO 구조**:
```json
{
  "ingredientId": "number",
  "quantity": "number",
  "unit": "string",
  "purchaseDate": "string (yyyy-MM-dd)",
  "expiryDate": "string (yyyy-MM-dd)",
  "storageLocation": "string",
  "notes": "string"
}
```

---

## 🍽️ 레시피 추천 API

### POST `/api/recipe-recommendations/refrigerator?userId={userId}`
**설명**: 냉장고 재료 기반 레시피 추천
```bash
curl -X POST "http://localhost:8080/api/recipe-recommendations/refrigerator?userId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "refrigeratorId": 1
  }'
```

### POST `/api/recipe-recommendations/text?userId={userId}&prompt={프롬프트}`
**설명**: 텍스트 기반 레시피 추천 (URL 인코딩 필요)
```bash
curl -X POST "http://localhost:8080/api/recipe-recommendations/text?userId=1&prompt=%EC%96%91%ED%8C%8C%EB%A1%9C%20%EB%A7%8C%EB%93%A4%20%EC%88%98%20%EC%9E%88%EB%8A%94%20%EC%9A%94%EB%A6%AC%20%EC%B6%94%EC%B2%9C%ED%95%B4%EC%A4%98"
```

**RecipeRecommendationRequestDTO 구조**:
```json
{
  "refrigeratorId": "number"
}
```

**응답 예시**:
```json
{
  "recipeId": 13,
  "title": "양파 스프",
  "description": "양파를 활용한 간단한 스프 레시피",
  "cookingTime": 30,
  "difficulty": "초급",
  "category": "스프"
}
```

---

## 💬 사용자 프롬프트 API

### GET `/api/users/{userId}/prompts`
**설명**: 사용자의 프롬프트 목록 조회
```bash
curl -X GET http://localhost:8080/api/users/1/prompts
```

### POST `/api/users/{userId}/prompts`
**설명**: 사용자 프롬프트 생성
```bash
curl -X POST http://localhost:8080/api/users/1/prompts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 프롬프트",
    "age": 25,
    "gender": "M",
    "allergy": "없음",
    "healthStatus": "건강",
    "isPregnant": false,
    "preference": "한식"
  }'
```

**UserPrompt 엔티티 구조**:
```json
{
  "id": "number",
  "name": "string",
  "age": "number",
  "gender": "string (M/F)",
  "allergy": "string",
  "healthStatus": "string",
  "isPregnant": "boolean",
  "preference": "string",
  "userId": "number"
}
```

---

## 💭 채팅 API

### GET `/api/chat/rooms`
**설명**: 채팅방 목록 조회
```bash
curl -X GET http://localhost:8080/api/chat/rooms
```

### POST `/api/chat/rooms`
**설명**: 채팅방 생성
```bash
curl -X POST http://localhost:8080/api/chat/rooms \
  -H "Content-Type: application/json" \
  -d '"테스트 채팅방"'
```

### GET `/api/chat/history?roomId={roomId}`
**설명**: 채팅 히스토리 조회
```bash
curl -X GET "http://localhost:8080/api/chat/history?roomId=1"
```

### POST `/api/chat/history`
**설명**: 채팅 메시지 전송

> **냉장고 재료 기반 레시피 추천**
>
> `refrigeratorId`를 함께 전달하면 해당 냉장고의 재료를 우선적으로 활용한 레시피를 추천합니다.
> - 냉장고에 재료가 없으면 안내 메시지가 반환됩니다.
> - `refrigeratorId`를 생략하면 기존 방식(재료 미반영)으로 동작합니다.

**예시: 냉장고 재료 기반 추천**
```bash
curl -X POST http://localhost:8080/api/chat/history \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "roomId": 1,
    "refrigeratorId": 5,
    "message": "오늘 저녁 뭐 먹을까?"
  }'
```

**예시: 기존 방식(냉장고 미지정)**
```bash
curl -X POST http://localhost:8080/api/chat/history \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "roomId": 1,
    "message": "오늘 저녁 뭐 먹을까?"
  }'
```

**ChatRequestDTO 구조**:
```json
{
  "userId": "number",
  "roomId": "number",
  "refrigeratorId": "number (optional)",
  "message": "string"
}
```

**응답 예시**:
```json
{
  "userMessage": {
    "id": 1,
    "message": "양파로 만들 수 있는 요리 추천해줘",
    "isUserMessage": true,
    "timestamp": "2024-12-20T10:30:00"
  },
  "aiResponse": {
    "id": 2,
    "message": "양파를 활용한 맛있는 요리를 추천해드릴게요...",
    "isUserMessage": false,
    "timestamp": "2024-12-20T10:30:05",
    "recipe": {
      "recipeId": 14,
      "title": "양파 스프",
      "description": "양파를 활용한 간단한 스프 레시피"
    }
  }
}
```

---

## 🔍 검색 API

### 재료 검색
```bash
# URL 인코딩된 한글 검색어 사용
curl -X GET "http://localhost:8080/api/ingredients/search?query=%EC%96%91%ED%8C%8C"
```

### 냉장고 재료 검색
```bash
# URL 인코딩된 한글 검색어 사용
curl -X GET "http://localhost:8080/api/refrigerators/1/ingredients/search?q=%EC%96%91%ED%8C%8C"
```

---

## 📊 DTO 구조

### UserDTO
```json
{
  "id": "number",
  "username": "string",
  "name": "string",
  "age": "number",
  "gender": "string",
  "preferences": "string",
  "healthConditions": "string",
  "allergies": "string",
  "email": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### IngredientDTO
```json
{
  "ingredientId": "number",
  "name": "string",
  "calories": "number",
  "nutritionInfo": "string",
  "requiredAmount": "number"
}
```

### RefrigeratorDTO
```json
{
  "refrigeratorId": "number",
  "name": "string",
  "userId": "number",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### RefrigeratorIngredientDTO
```json
{
  "id": "number",
  "refrigeratorId": "number",
  "ingredientId": "number",
  "quantity": "number",
  "unit": "string",
  "purchaseDate": "string (yyyy-MM-dd)",
  "expiryDate": "string (yyyy-MM-dd)",
  "storageLocation": "string",
  "notes": "string",
  "freshnessStatus": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "ingredient": {
    "ingredientId": "number",
    "name": "string",
    "calories": "number",
    "nutritionInfo": "string",
    "requiredAmount": "number"
  }
}
```

### RecipeResponseDTO
```json
{
  "recipeId": "number",
  "title": "string",
  "description": "string",
  "cookingTime": "number",
  "difficulty": "string",
  "category": "string",
  "image": "string (base64)",
  "ingredients": [
    {
      "ingredientId": "number",
      "name": "string",
      "quantity": "number",
      "unit": "string"
    }
  ]
}
```

---

## ⚠️ 주의사항

### 1. URL 인코딩
한글 검색어를 사용할 때는 반드시 URL 인코딩을 해야 합니다:
```bash
# 잘못된 예
curl "http://localhost:8080/api/ingredients/search?query=양파"

# 올바른 예
curl "http://localhost:8080/api/ingredients/search?query=%EC%96%91%ED%8C%8C"
```

### 2. 날짜 형식
- `purchaseDate`, `expiryDate`: `yyyy-MM-dd` 형식
- `createdAt`, `updatedAt`: ISO 8601 형식 (`yyyy-MM-ddTHH:mm:ss`)

### 3. Perplexity API
현재 Perplexity API 키가 설정되지 않아 테스트 응답을 반환합니다. 실제 API 키를 설정하면 AI 기반 레시피 추천이 작동합니다.

### 4. Jackson 직렬화
LocalDateTime 필드는 자동으로 ISO 8601 형식으로 직렬화됩니다.

---

## 🧪 테스트 시나리오

### 기본 워크플로우
1. **사용자 등록**
2. **냉장고 생성**
3. **재료 생성**
4. **냉장고에 재료 추가**
5. **레시피 추천 요청**
6. **채팅으로 추가 질문**

### 예시 명령어
```bash
# 1. 사용자 등록
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","name":"테스트 사용자","age":25,"gender":"남성","preferences":"한식","healthConditions":"없음","allergies":"없음"}'

# 2. 냉장고 생성
curl -X POST http://localhost:8080/api/refrigerators \
  -H "Content-Type: application/json" \
  -d '{"name":"테스트 냉장고","userId":1}'

# 3. 재료 생성
curl -X POST http://localhost:8080/api/ingredients \
  -H "Content-Type: application/json" \
  -d '{"name":"양파","calories":40,"nutritionInfo":"비타민C, 식이섬유","requiredAmount":100.0}'

# 4. 냉장고에 재료 추가
curl -X POST http://localhost:8080/api/refrigerators/1/ingredients \
  -H "Content-Type: application/json" \
  -d '{"ingredientId":1,"quantity":2.5,"unit":"개","purchaseDate":"2024-12-20","expiryDate":"2024-12-31","storageLocation":"냉장실","notes":"양파 2.5개"}'

# 5. 레시피 추천
curl -X POST "http://localhost:8080/api/recipe-recommendations/refrigerator?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"refrigeratorId":1}'

# 6. 채팅 메시지 전송
curl -X POST http://localhost:8080/api/chat/history \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"roomId":1,"refrigeratorId":5,"message":"오늘 저녁 뭐 먹을까?"}'
```

---

## 📝 에러 코드

- `200`: 성공
- `400`: 잘못된 요청 (DTO 검증 실패)
- `404`: 리소스를 찾을 수 없음
- `500`: 서버 내부 오류

모든 API는 JSON 형식으로 응답하며, 에러 시에도 일관된 형식의 에러 메시지를 반환합니다. 