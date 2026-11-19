package jandi.domain;

import java.time.LocalDateTime;

/**
 * 로컬 git 저장소에서 읽은 커밋 한 건.
 *
 * @param hash        커밋 해시
 * @param authorName  작성자 이름
 * @param authorEmail 작성자 이메일
 * @param time        작성 시각(이 컴퓨터 시간대)
 * @param subject     커밋 메시지 첫 줄
 * @param added       추가한 줄 수
 * @param deleted     지운 줄 수
 * @author Team 3
 * @version 0.1
 */
public record Commit(String hash, String authorName, String authorEmail, LocalDateTime time,
                     String subject, int added, int deleted) {

    /** @return 바꾼 줄 수(추가 + 삭제) */
    public int lines() {
        return added + deleted;
    }
}
