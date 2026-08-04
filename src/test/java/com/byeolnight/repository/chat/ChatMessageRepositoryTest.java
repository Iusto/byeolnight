package com.byeolnight.repository.chat;

import com.byeolnight.entity.chat.ChatMessage;
import com.byeolnight.infrastructure.config.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@DisplayName("채팅 커서 페이지네이션")
class ChatMessageRepositoryTest {

    private static final String ROOM = "public";
    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    @Autowired
    ChatMessageRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    /**
     * id 순서와 timestamp 순서가 어긋난 메시지를 저장한다.
     *
     * <p>id는 INSERT 시점에 DB가 채번하고 timestamp는 그 전에 애플리케이션이 채우므로,
     * 거의 동시에 도착한 메시지끼리는 두 순서가 뒤집힐 수 있다.
     * 10건마다 한 건의 시각을 뒤로 밀어 그 상황을 만든다.
     *
     * @return 저장한 순서(=id 오름차순) 그대로의 목록
     */
    private List<ChatMessage> saveMessagesWithInvertedTimestamps(int count) {
        List<ChatMessage> saved = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            saved.add(repository.save(ChatMessage.builder()
                    .roomId(ROOM)
                    .sender("user-" + i)
                    .message("메시지 " + i)
                    .isBlinded(false)
                    .timestamp(BASE)
                    .build()));
        }
        repository.flush();

        // @CreatedDate가 저장 시각으로 덮어쓰므로 저장 후에 시각을 조정한다.
        // 10건마다 한 건을 35초 뒤로 밀어, 뒤에 저장된 메시지보다 시각이 늦은 상태를 만든다.
        for (int i = 0; i < count; i++) {
            LocalDateTime timestamp = BASE.plusSeconds(i * 10L)
                    .plusSeconds(i % 10 == 0 ? 35L : 0L);
            entityManager.getEntityManager()
                    .createQuery("UPDATE ChatMessage c SET c.timestamp = :ts WHERE c.id = :id")
                    .setParameter("ts", timestamp)
                    .setParameter("id", saved.get(i).getId())
                    .executeUpdate();
        }
        entityManager.clear();
        return saved;
    }

    /** 첫 페이지부터 끝까지 넘기며 조회된 id를 순서대로 모은다. */
    private List<Long> pageThroughAll(int pageSize) {
        List<Long> collected = new ArrayList<>();

        List<ChatMessage> firstPage =
                repository.findRecentByRoomIdOrderByIdDesc(ROOM, PageRequest.of(0, pageSize));
        firstPage.forEach(m -> collected.add(m.getId()));

        // 커서 = 화면에 표시된 것 중 가장 오래된 메시지의 id
        Long cursor = collected.get(collected.size() - 1);
        while (true) {
            List<ChatMessage> page = repository.findByRoomIdAndIdLessThanOrderByIdDesc(
                    ROOM, cursor, PageRequest.of(0, pageSize));
            if (page.isEmpty()) {
                break;
            }
            page.forEach(m -> collected.add(m.getId()));
            cursor = page.get(page.size() - 1).getId();
        }
        return collected;
    }

    @Test
    @DisplayName("id 순서와 timestamp 순서가 어긋나도 중복·누락 없이 전부 조회된다")
    void pagesWithoutDuplicateOrLoss() {
        List<ChatMessage> saved = saveMessagesWithInvertedTimestamps(100);

        List<Long> collected = pageThroughAll(10);

        assertThat(collected)
                .as("같은 메시지가 두 번 나오면 안 된다")
                .doesNotHaveDuplicates();
        assertThat(collected)
                .as("저장한 메시지가 하나도 빠지면 안 된다")
                .containsExactlyInAnyOrderElementsOf(saved.stream().map(ChatMessage::getId).toList());
        assertThat(collected)
                .as("최신순으로 이어져야 한다")
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("정렬 기준은 timestamp가 아니라 커서와 같은 id다")
    void ordersByIdNotTimestamp() {
        List<ChatMessage> saved = saveMessagesWithInvertedTimestamps(30);
        Long cursor = saved.get(25).getId();

        List<ChatMessage> page = repository.findByRoomIdAndIdLessThanOrderByIdDesc(
                ROOM, cursor, PageRequest.of(0, 5));

        assertThat(page).extracting(ChatMessage::getId)
                .containsExactly(
                        saved.get(24).getId(), saved.get(23).getId(), saved.get(22).getId(),
                        saved.get(21).getId(), saved.get(20).getId());

        // 이 구간은 timestamp 순서가 id 순서와 다르다.
        // timestamp로 정렬했다면 같은 결과가 나오지 않는다는 뜻이고,
        // 그 불일치가 커서를 어긋나게 만들던 원인이다.
        List<LocalDateTime> timestamps = page.stream().map(ChatMessage::getTimestamp).toList();
        List<LocalDateTime> descending = timestamps.stream()
                .sorted(Comparator.reverseOrder()).toList();
        assertThat(timestamps)
                .as("id 순서와 timestamp 순서가 어긋난 구간이어야 이 테스트가 의미를 갖는다")
                .isNotEqualTo(descending);
    }
}
