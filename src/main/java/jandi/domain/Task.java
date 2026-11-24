package jandi.domain;

import java.time.LocalDateTime;

/**
 * 할 일 하나. 목장에서는 양 한 마리로 보인다.
 *
 * @author Team 3
 * @version 0.1
 */
public class Task {

    private final String id;
    private final String ownerId;
    private final String title;
    private String memo;
    private TaskStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime submittedAt;

    /**
     * @param id          할 일 번호
     * @param ownerId     담당 팀원 번호
     * @param title       제목
     * @param memo        메모(한 일)
     * @param status      상태
     * @param createdAt   만든 시각
     * @param submittedAt 제출 시각(제출 전이면 null)
     */
    public Task(String id, String ownerId, String title, String memo, TaskStatus status,
                LocalDateTime createdAt, LocalDateTime submittedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.memo = memo == null ? "" : memo;
        this.status = status;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
    }

    /** @return 할 일 번호 */
    public String getId() {
        return id;
    }

    /** @return 담당 팀원 번호 */
    public String getOwnerId() {
        return ownerId;
    }

    /** @return 제목 */
    public String getTitle() {
        return title;
    }

    /** @return 메모 */
    public String getMemo() {
        return memo;
    }

    /** @param memo 메모 */
    public void setMemo(String memo) {
        this.memo = memo == null ? "" : memo;
    }

    /** @return 상태 */
    public TaskStatus getStatus() {
        return status;
    }

    /** @param status 상태 */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /** @return 만든 시각 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return 제출 시각(제출 전이면 null) */
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    /** @param submittedAt 제출 시각 */
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    /** @return 제출되어 양이 박스 안에 있으면 true */
    public boolean isSubmitted() {
        return status == TaskStatus.SUBMITTED;
    }
}
