# 채팅 커서 페이지네이션 측정 재현

[16_chat-cursor-pagination.md](../../16_chat-cursor-pagination.md)의 수치를 그대로 다시 뽑는 스크립트다.
로컬 Docker MySQL만 있으면 되고, 애플리케이션은 띄우지 않아도 된다.

## 실행

```bash
# 1. MySQL 8.0 기동
docker run -d --name byeolnight-explain \
  -e MYSQL_ROOT_PASSWORD=explain1234 \
  -e MYSQL_DATABASE=byeolnight \
  -p 3307:3306 \
  mysql:8.0 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# 2. 스키마 + 데이터 (수정 전 인덱스 상태로 생성)
docker exec -i byeolnight-explain mysql -uroot -pexplain1234 < 01_schema.sql
docker exec -i byeolnight-explain mysql -uroot -pexplain1234 --table < 02_seed.sql

# 3. 커서 페이지네이션 중복·누락 (수정 전 vs 수정 후)
docker exec -i byeolnight-explain mysql -uroot -pexplain1234 --table < 03_paging_sim.sql

# 4. 실행계획 — 수정 전
docker exec -i byeolnight-explain mysql -uroot -pexplain1234 --table < 04_explain_before.sql

# 5. 실행계획 — 수정 후 (인덱스 추가 포함)
docker exec -i byeolnight-explain mysql -uroot -pexplain1234 --table < 05_explain_after.sql

# 6. 기존 인덱스를 제거해도 되는지 확인
docker exec -i byeolnight-explain mysql -uroot -pexplain1234 --table < 06_old_index_check.sql

# 정리
docker rm -f byeolnight-explain
```

`06`은 인덱스를 실제로 지웠다가 재측정하므로, 이어서 다른 측정을 하려면
`01`부터 다시 실행하거나 인덱스를 복구해야 한다.

## 실행 순서 주의

`04` → `05` 순서를 지켜야 한다. `05`가 `(room_id, id DESC)` 인덱스를 추가하므로,
먼저 실행하면 "수정 전" 실행계획을 얻을 수 없다.

## 얻는 결과

| 스크립트 | 결과 |
|---|---|
| `02` | 30만 건, `public` 방 240,041건, id/timestamp 역전 1.000% |
| `03` | 수정 전 중복 5·누락 2 / 수정 후 0·0 (200페이지, 6,030행) |
| `04` | 30건 조회에 150,045행 스캔, 154ms |
| `05` | 44행 스캔, 0.056ms. 메시지 적은 방은 2,502행 → 30행 |
| `06` | 기존 인덱스 제거 시 관리자 조회 0.39ms → 41.6ms (제거 불가) |

## 한계

로컬 재현 환경이며 운영 DB 실측이 아니다.
id/timestamp 역전 비율 1%는 측정값이 아니라 "동시 도착이 이 정도 있다면"을 가정한 값이다.
이 실험의 목적은 비율을 특정하는 것이 아니라, **어떤 비율에서든 문제가 발생하며
수정 후에는 발생하지 않는다**는 것을 보이는 데 있다.
