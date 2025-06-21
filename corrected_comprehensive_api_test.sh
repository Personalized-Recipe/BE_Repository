#!/bin/bash

BASE_URL="http://localhost:8080/api"
echo "=== 종합 API 테스트 시작 (수정 버전) ==="

# 1. Perplexity API 테스트
echo "1. Perplexity API 테스트"
curl -s -X GET "$BASE_URL/test/perplexity" | jq '.' || echo "Perplexity API 오류"

# 2. 사용자 관련 API 테스트
echo -e "\n2. 사용자 관련 API 테스트"
echo "2.1 사용자 목록 조회"
curl -s -X GET "$BASE_URL/users" | jq '.' || echo "사용자 목록 조회 오류"

echo -e "\n2.2 사용자 상세 조회 (ID: 1)"
curl -s -X GET "$BASE_URL/users/1" | jq '.' || echo "사용자 상세 조회 오류"

# 3. 재료 관련 API 테스트
echo -e "\n3. 재료 관련 API 테스트"
echo "3.1 재료 검색 (감자)"
curl -s -G -X GET "$BASE_URL/ingredients/search" --data-urlencode "query=감자" | jq '.' || echo "재료 검색 오류"

echo -e "\n3.2 재료 상세 조회 (ID: 1)"
curl -s -X GET "$BASE_URL/ingredients/1" | jq '.' || echo "재료 상세 조회 오류"

# 4. 냉장고 관련 API 테스트
echo -e "\n4. 냉장고 관련 API 테스트"
echo "4.1 냉장고 생성 (사용자 ID: 1)"
curl -s -X POST "$BASE_URL/refrigerators" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 냉장고",
    "userId": 1
  }' | jq '.' || echo "냉장고 생성 오류"

echo -e "\n4.2 특정 사용자 냉장고 목록 조회 (ID: 1)"
curl -s -X GET "$BASE_URL/refrigerators/user/1" | jq '.' || echo "사용자 냉장고 목록 조회 오류"

# 5. 냉장고 재료 관련 API 테스트
echo -e "\n5. 냉장고 재료 관련 API 테스트"
echo "5.1 냉장고 재료 추가 (냉장고 ID: 1)"
curl -s -X POST "$BASE_URL/refrigerators/1/ingredients" \
  -H "Content-Type: application/json" \
  -d '{
    "ingredientId": 1,
    "quantity": 500,
    "unit": "그램",
    "expiryDate": "2024-12-31",
    "freshness": "GOOD"
  }' | jq '.' || echo "냉장고 재료 추가 오류"

echo -e "\n5.2 냉장고 재료 목록 조회 (냉장고 ID: 1)"
curl -s -X GET "$BASE_URL/refrigerators/1/ingredients" | jq '.' || echo "냉장고 재료 목록 조회 오류"

# 6. 레시피 관련 API 테스트
echo -e "\n6. 레시피 관련 API 테스트"
echo "6.1 레시피 검색 (감자)"
curl -s -G -X GET "$BASE_URL/recipes/search" --data-urlencode "query=감자" | jq '.' || echo "레시피 검색 오류"

echo -e "\n6.2 레시피 상세 조회 (ID: 1)"
curl -s -X GET "$BASE_URL/recipes/1" | jq '.' || echo "레시피 상세 조회 오류"

# 7. 레시피 추천 API 테스트
echo -e "\n7. 레시피 추천 API 테스트"
echo "7.1 텍스트 기반 레시피 추천"
curl -s -X POST "$BASE_URL/recipe-recommendations/text?userId=1&prompt=감자로 만들 수 있는 간단한 요리" \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.' || echo "텍스트 기반 레시피 추천 오류"

echo -e "\n7.2 냉장고 기반 레시피 추천"
curl -s -X POST "$BASE_URL/recipe-recommendations/refrigerator?userId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "refrigeratorId": 1
  }' | jq '.' || echo "냉장고 기반 레시피 추천 오류"

# 8. 프롬프트 관련 API 테스트
echo -e "\n8. 프롬프트 관련 API 테스트"
echo "8.1 프롬프트 생성 (사용자 ID: 1)"
curl -s -X POST "$BASE_URL/users/1/prompts" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "감자로 만들 수 있는 간단한 요리 추천해줘",
    "category": "RECIPE"
  }' | jq '.' || echo "프롬프트 생성 오류"

echo -e "\n8.2 사용자 프롬프트 목록 조회 (ID: 1)"
curl -s -X GET "$BASE_URL/users/1/prompts" | jq '.' || echo "사용자 프롬프트 목록 조회 오류"

# 9. 채팅 관련 API 테스트
echo -e "\n9. 채팅 관련 API 테스트"
echo "9.1 채팅방 생성"
curl -s -X POST "$BASE_URL/chat/rooms" \
  -H "Content-Type: application/json" \
  -d '"요리 상담"' | jq '.' || echo "채팅방 생성 오류"

echo -e "\n9.2 채팅 메시지 전송"
curl -s -X POST "$BASE_URL/chat/history" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "roomId": 1,
    "message": "감자 요리 추천해줘"
  }' | jq '.' || echo "채팅 메시지 전송 오류"

echo -e "\n=== 종합 API 테스트 완료 ===" 