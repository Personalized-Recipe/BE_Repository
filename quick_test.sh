#!/bin/bash

BASE_URL="http://localhost:8080"
echo "=== 빠른 API 테스트 ==="

# 1. Perplexity API 테스트
echo "1. Perplexity API 테스트"
curl -s -X GET "$BASE_URL/test/perplexity" | head -c 200

# 2. 사용자 목록 조회
echo -e "\n\n2. 사용자 목록 조회"
curl -s -X GET "$BASE_URL/users" | head -c 200

# 3. 재료 검색
echo -e "\n\n3. 재료 검색"
curl -s -X GET "$BASE_URL/ingredients/search?query=감자" | head -c 200

echo -e "\n\n=== 테스트 완료 ===" 