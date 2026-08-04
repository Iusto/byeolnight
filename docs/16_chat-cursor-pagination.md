# 채팅 무한 스크롤 커서 페이지네이션 수정

## 요약

채팅 이력 조회에서 **커서 키와 정렬 키가 서로 달랐다.** 커서는 `id`인데 정렬은 `timestamp`였다.
이 때문에 두 가지 문제가 있었다.

1. **정확성** — 같은 메시지가 두 번 나오거나, 영영 조회되지 않는 메시지가 생긴다.
2. **성능** — 30건을 뽑기 위해 15만 행을 스캔했다. 스크롤을 내릴수록 더 느려진다.

정렬 기준을 커서와 같은 `id`로 통일하고, `(room_id, id DESC)` 인덱스를 추가해 해결했다.

---

## 1. 무엇이 잘못됐나

같은 기능인데 첫 페이지와 다음 페이지의 정렬 기준이 달랐다.

```java
// 첫 페이지
@Query("SELECT c FROM ChatMessage c WHERE c.roomId = :roomId ORDER BY c.id DESC")
List<ChatMessage> findRecentByRoomIdOrderByIdDesc(...);

// 다음 페이지  ← 필터는 id, 정렬은 timestamp
List<ChatMessage> findByRoomIdAndIdLessThanOrderByTimestampDesc(...);
```

커서 페이지네이션은 "정렬 순서상 여기까지 봤다"를 커서로 표현한다.
그래서 **커서 키는 정렬 키와 같아야 한다.** 다르면 커서가 정렬 순서를 따라가지 못한다.

### 왜 id와 timestamp의 순서가 어긋나나

- `id` — INSERT 시점에 DB가 채번한다.
- `timestamp` — `@CreatedDate`가 flush **이전에** 애플리케이션에서 채운다.

두 메시지가 거의 동시에 도착하면 채번 순서와 `@CreatedDate` 순서가 뒤집힐 수 있다.
평소에는 두 순서가 같아 보이지만 동시 삽입 구간에서 어긋난다.

---

## 2. 재현 실험

### 환경

| 항목 | 값 |
|---|---|
| DB | MySQL 8.0 (Docker, 로컬) |
| 데이터 | `chat_messages` 30만 건 |
| 방 분포 | `public` 240,041건(80%), 나머지 19개 방에 분산 |
| id/timestamp 역전 비율 | 1.000% (2,400 / 240,041 쌍) |

> **한계**: 로컬 재현 환경이며 운영 DB 실측이 아니다.
> 역전 비율 1%는 실제 측정값이 아니라 "동시 도착이 이 정도 비율로 있다면"을 가정한 값이다.
> 실제 비율은 트래픽에 따라 달라진다. 이 실험은 **비율이 얼마든 문제가 발생한다는 사실**과
> **수정 후에는 발생하지 않는다는 사실**을 보이는 것이 목적이다.

### 정확성: 200페이지를 실제로 넘겨본 결과

첫 페이지부터 200페이지까지, 매 페이지의 커서로 다음 페이지를 조회하는 과정을 그대로 재현했다.

| 방식 | 반환 행수 | 중복 | 영구 누락 |
|---|---|---|---|
| 수정 전 (커서 `id`, 정렬 `timestamp`) | 6,030 | **5** | **2** |
| 수정 후 (커서 `id`, 정렬 `id`) | 6,030 | 0 | 0 |

"영구 누락"은 다음 커서가 이미 그 행을 지나쳐버려 **이후 어떤 요청으로도 조회할 수 없는** 메시지를 뜻한다.
사용자 입장에서는 스크롤을 올려도 그 메시지만 사라진 것처럼 보인다.

### 성능: 실행계획

**수정 전** — `ORDER BY timestamp DESC`

```
-> Limit: 30 row(s)  (actual time=154..154 rows=30 loops=1)
    -> Filter: room_id='public' and id < 150000  (actual time=154..154 rows=30)
        -> Index scan on chat_messages using idx_chat_timestamp (reverse)
                                             (actual time=0.0363..139 rows=150045)
```

옵티마이저는 `ORDER BY timestamp DESC`를 만족시키려 `idx_chat_timestamp`를 역방향으로 훑는다.
그러면서 `id < 150000` 조건에 맞지 않는 행을 계속 버린다.
**30건을 채우려고 150,045행을 읽었다.**

커서를 더 깊이 넣으면 그만큼 더 읽는다.

| 커서 | 스캔한 행 | 소요 |
|---|---|---|
| `id < 150000` | 150,045 | 154ms |
| `id < 20000` | 280,035 | **341ms** |

스크롤을 내릴수록 느려지는 구조다.

**수정 후** — `ORDER BY id DESC`

```
-> Limit: 30 row(s)  (actual time=0.0303..0.0558 rows=30 loops=1)
    -> Filter: room_id='public' and id < 150000  (actual time=0.03..0.0543 rows=30)
        -> Index range scan on chat_messages using PRIMARY over (id < 150000) (reverse)
                                             (actual time=0.0259..0.0466 rows=44)
```

**150,045행 → 44행, 154ms → 0.056ms.**
커서를 깊이 넣어도 마찬가지다(`id < 20000`: 34행, 0.042ms).

정렬 키를 커서와 맞추자 PK를 범위 스캔으로 쓸 수 있게 되면서, 필요한 위치로 바로 진입한다.

---

## 3. 인덱스

`(room_id, id DESC)` 인덱스를 추가했다.

정렬 기준만 고쳐도 `public` 방은 이미 충분히 빠르다.
`public`이 전체의 80%라 PK를 역방향으로 조금만 읽어도 30건이 채워지기 때문이다.
문제는 **메시지가 적은 방**이다.

| 방 | 인덱스 없음 | `(room_id, id DESC)` 추가 후 |
|---|---|---|
| `public` (80%) | 44행 / 0.056ms | 44행 / 0.046ms (옵티마이저가 PK 유지) |
| `room-15` (1%) | 2,502행 / 1.21ms | **30행 / 0.109ms** |

`room-15`는 PK를 역방향으로 읽으며 다른 방 메시지를 계속 버려야 해서 2,502행을 읽었다.
인덱스를 추가하면 해당 방의 구간으로 바로 진입해 30행만 읽는다.

현재는 사실상 `public` 방 하나만 쓰므로 체감 효과는 크지 않다.
**방이 늘어날 때 느려지지 않도록 하는 인덱스**로 이해하는 것이 정확하다.

### 기존 인덱스는 유지했다

`idx_chat_room_timestamp (is_blinded, room_id, timestamp)`는 채팅 조회에 쓰이지 않는다.
선두 컬럼 `is_blinded`가 조회 조건에 없어서 B-Tree를 탐색할 수 없기 때문이다.

그렇다고 쓸모없는 인덱스는 아니었다. 관리자 블라인드 조회가 선두 컬럼을 쓰고 있었다.

| 쿼리 | 인덱스 유지 | 제거 시 |
|---|---|---|
| `countByIsBlindedTrue()` | 0.39ms | 41.6ms (풀스캔) |
| `findByIsBlindedTrueOrderByBlindedAtDesc()` | 7.31ms | 93.8ms (풀스캔) |

제거 대신 유지하고, 채팅 조회용 인덱스를 따로 추가하는 쪽을 택했다.

> 참고: 이 인덱스는 `is_blinded` 하나만 실제로 쓰이고 뒤의 두 컬럼은 놀고 있다.
> `(is_blinded, blinded_at DESC)`로 좁히면 위 두 번째 쿼리의 정렬도 없앨 수 있지만,
> 이번 수정 범위를 넘어서므로 그대로 두었다.

---

## 4. 변경 내역

**`ChatMessageRepository`**

```java
// 변경 전
List<ChatMessage> findByRoomIdAndIdLessThanOrderByTimestampDesc(...);
// 변경 후
List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(...);
```

**`ChatMessage`** — 인덱스 추가

```java
@Index(name = "idx_chat_room_id", columnList = "room_id, id DESC")
```

`ddl-auto: update`가 아닌 환경이라면 직접 적용한다.

```sql
CREATE INDEX idx_chat_room_id ON chat_messages (room_id, id DESC);
```

**`ChatService`** — 커서 파싱 가드

`Long.parseLong(beforeId)`를 그대로 호출해 숫자가 아닌 커서가 들어오면
`NumberFormatException`이 500으로 나갔다. 클라이언트가 보내는 값이므로 400이 맞다.

```java
private Long parseCursor(String beforeId) {
    try {
        return Long.parseLong(beforeId);
    } catch (NumberFormatException e) {
        throw new InvalidRequestException("beforeId는 숫자여야 합니다: " + beforeId);
    }
}
```

---

## 5. 테스트

**`ChatMessageRepositoryTest`** — id 순서와 timestamp 순서가 어긋난 데이터를 만들고,
첫 페이지부터 끝까지 넘기며 중복·누락이 없는지 검증한다.

`@CreatedDate` 감사가 저장 시각으로 덮어쓰기 때문에, 저장 후 JPQL로 시각을 조정해
역전 상황을 만든다.

**`ChatServiceTest`** — 숫자가 아닌 커서, 빈 커서가 `InvalidRequestException`이 되는지,
정상 커서가 id 기준으로 조회되는지 검증한다.

---

## 6. 배운 것

커서 페이지네이션에서 **커서 키와 정렬 키는 반드시 같아야 한다.**
이번 경우 같은 파일 안에서 첫 페이지는 `id`, 다음 페이지는 `timestamp`로 정렬하고 있었는데,
평소에는 두 순서가 거의 같아 증상이 드러나지 않았다.

만약 정렬 기준을 꼭 `timestamp`로 두어야 했다면 커서도 `timestamp` 하나로는 부족하다.
같은 시각의 메시지가 여러 건일 수 있으므로 `(timestamp, id)` 튜플 비교가 필요하다.

```sql
WHERE room_id = ?
  AND (`timestamp`, id) < (?, ?)
ORDER BY `timestamp` DESC, id DESC
```

채팅은 id 순서로 보여줘도 문제가 없어서 더 단순한 쪽을 택했다.
