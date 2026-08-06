# 12. 이메일 비동기 처리와 재시도

## 구조

- 신규 작업: Redis List `queue:mail`
- 지연 재시도: Redis Sorted Set `queue:mail:retry`
- 최종 실패: Redis List `queue:mail:dlq`
- 소비자: `EmailWorker` (`@Scheduled(fixedDelay = 1000)`)

API는 인증 코드를 해시해 Redis에 저장하고 `EmailJob`을 신규 작업 큐에 넣은 뒤 즉시 응답한다. SMTP 전송은 워커가 담당한다.

## 지연 재시도

실패 작업을 신규 작업 큐에 즉시 다시 넣지 않는다. 실패 횟수에 따라 다음 실행 시각을 계산하고 Sorted Set score에 epoch milliseconds로 저장한다.

| 실패 후 attempt | 지연 |
|---:|---:|
| 1 | 5초 |
| 2 | 10초 |
| 3 | 20초 |
| 4 | 40초 |
| 5 | DLQ 이동 |

총 전송 시도는 최초 전송을 포함해 5회다. `EmailJob`은 `lastAttemptAt`, `nextAttemptAt`, `errorMessage`를 기록해 운영 시 재시도 상태를 확인할 수 있다.

## 원자적 due-pop

`RedisCacheService.dequeueDue()`는 Lua 스크립트에서 다음 연산을 한 번에 수행한다.

1. `ZRANGEBYSCORE ... LIMIT 0 1`로 실행 시각이 지난 작업 조회
2. `ZREM` 성공 시에만 JSON 반환

따라서 여러 애플리케이션 인스턴스가 동시에 실행되어도 같은 재시도 작업을 중복 소비하지 않는다. 실행 가능한 재시도 작업이 없을 때만 신규 작업 List를 blocking pop한다.

## 실패 정책

- SMTP 실패: 지수 백오프로 지연 큐 예약
- 다섯 번째 실패: 최종 attempt와 실패 시각을 기록해 DLQ 이동
- Redis 직렬화 실패: 오류로 처리하고 로그 기록
- 워커 루프 예외: 스케줄러가 중단되지 않도록 작업 단위로 로깅

## 남은 한계

Redis List는 처리 확인(ACK)이 없어 워커가 작업을 꺼낸 직후 종료되면 메일이 유실될 수 있다. 인증 메일은 사용자가 재요청할 수 있어 현재 규모에서는 수용했으며, 유실이 중요해지면 처리 중 큐나 ACK 지원 방식으로 전환할 계획이다.

## 검증

`EmailWorkerTest`가 다음 계약을 고정한다.

- 실패 작업을 신규 큐에 즉시 재삽입하지 않음
- 5/10/20/40초 백오프
- due 재시도 우선 처리
- 다섯 번째 실패의 DLQ 이동과 최종 attempt 기록
