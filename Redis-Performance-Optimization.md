# Redis를 활용한 레시피 추천 시스템 성능 최적화 문서

## 1. 개요
이 문서는 레시피 추천 시스템에서 Redis 캐시를 활용하여 성능을 최적화하는 방법에 대해 설명합니다. Redis는 메모리 기반 데이터 저장소로, 빠른 읽기/쓰기 성능을 제공하여 시스템의 응답성을 향상시킵니다.

## 2. Redis 설정

### 2.1 기본 설정 (application.yml)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000
      client-type: lettuce
      connect-timeout: 3000
      lettuce:
        pool:
          enabled: true
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1
```

### 2.2 Redis 구성 클래스
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return template;
    }
}
```

## 3. 주요 캐싱 전략

### 3.1 채팅 내역 캐싱
- **목적**: 사용자와 시스템 간의 대화 내역을 빠르게 조회
- **키 구조**: `chat:session:{userId}:{sessionId}`
- **데이터 구조**: List 타입 (시간순 메시지 저장)
- **TTL**: 24시간

```java
// 채팅 저장 예시
String cacheKey = "chat:session:" + userId + ":" + sessionId;
redisTemplate.opsForList().rightPush(cacheKey, chatHistory);
redisTemplate.expire(cacheKey, 24 * 60 * 60, TimeUnit.SECONDS);
```

### 3.2 세션 관리
- **목적**: 사용자별 현재 활성 세션 추적
- **키 구조**: `user:session:{userId}`
- **데이터 구조**: String 타입 (세션 ID 저장)

```java
// 세션 ID 조회/생성 예시
String cacheKey = "user:session:" + userId;
String sessionId = (String) redisTemplate.opsForValue().get(cacheKey);
if (sessionId == null) {
    sessionId = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(cacheKey, sessionId);
}
```

## 4. 성능 최적화 효과

### 4.1 읽기 성능 향상
- Redis 캐시에서 먼저 조회하여 DB 부하 감소
- 캐시 미스(Cache Miss)일 경우에만 DB 조회

```java
// 캐시 우선 조회 패턴
List<Object> cachedChats = redisTemplate.opsForList().range(cacheKey, 0, -1);
if (cachedChats != null && !cachedChats.isEmpty()) {
    return cachedChats.stream().map(chat -> (ChatHistory) chat).toList();
}
// 캐시에 없으면 DB 조회
return chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
```

### 4.2 실시간 대화 응답 속도 개선
- 메모리 기반 저장으로 밀리초 단위의 응답 시간 제공
- Perplexity API 응답과 함께 빠른 대화 경험 제공

### 4.3 데이터 일관성 유지
- Redis와 MySQL에 동시 저장하여 데이터 손실 방지
- 캐시 만료 후에도 DB에서 데이터 조회 가능

### 4.4 자동 데이터 관리
- TTL 설정으로 오래된 캐시 자동 정리
- 스케줄링된 작업으로 30일 이상 된 데이터 아카이빙

```java
@Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
public void archiveOldChats() {
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    chatHistoryRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
}
```

## 5. 구현 시 고려사항

### 5.1 직렬화 전략
- JSON 직렬화를 통해 객체를 Redis에 저장
- `GenericJackson2JsonRedisSerializer` 사용으로 클래스 정보 포함

### 5.2 메모리 관리
- TTL 설정으로 메모리 사용량 제한
- 커넥션 풀 설정으로 리소스 효율적 관리

### 5.3 장애 대응
- Redis 장애 시 DB 조회로 폴백(fallback) 처리
- 이중 저장 방식으로 데이터 안정성 확보

## 6. 결론
Redis 캐시를 활용하여 레시피 추천 시스템의 응답 속도를 크게 향상시키고, 사용자 경험을 개선했습니다. 특히 채팅 기반 인터페이스에서 실시간성이 중요한 상황에서 효과적인 성능 최적화 방법을 제공합니다. 