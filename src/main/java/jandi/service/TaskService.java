package jandi.service;

import jandi.domain.Task;
import jandi.domain.TaskStatus;
import jandi.store.DataStore;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 할 일을 추가하고, 완료하고, 제출한다. 자기 할 일만 바꿀 수 있다.
 *
 * @author Team 3
 * @version 0.1
 */
public class TaskService {

    private final DataStore store;

    /** @param store 저장소 */
    public TaskService(DataStore store) {
        this.store = store;
    }

    /**
     * @param memberId 팀원 번호
     * @return 그 팀원의 할 일
     */
    public List<Task> tasksOf(String memberId) {
        return store.tasks().stream().filter(t -> t.getOwnerId().equals(memberId)).collect(Collectors.toList());
    }

    /**
     * 할 일을 추가한다.
     *
     * @param actorId 로그인한 팀원 번호
     * @param title   제목
     * @param memo    메모
     * @return 만든 할 일
     * @throws JandiException 제목이 비었을 때
     */
    public Task add(String actorId, String title, String memo) {
        if (title == null || title.isBlank()) {
            throw new JandiException("할 일 제목을 적어 주세요.");
        }
        Task t = store.createTask(actorId, title.trim(), memo == null ? "" : memo.trim());
        store.log(actorId, "할 일 추가", t.getId() + " " + t.getTitle());
        return t;
    }

    /**
     * 완료 체크를 켜거나 끈다.
     *
     * @param actorId 로그인한 팀원 번호
     * @param taskId  할 일 번호
     * @return 바뀐 할 일
     * @throws JandiException 남의 할 일이거나 이미 제출했을 때
     */
    public Task toggleDone(String actorId, String taskId) {
        Task t = own(actorId, taskId);
        if (t.isSubmitted()) {
            throw new JandiException("이미 제출한 할 일은 바꿀 수 없습니다.");
        }
        t.setStatus(t.getStatus() == TaskStatus.TODO ? TaskStatus.DONE : TaskStatus.TODO);
        store.saveTasks();
        store.log(actorId, "완료 체크", t.getId() + " " + t.getStatus());
        return t;
    }

    /**
     * 완료한 할 일을 제출한다. 양이 박스 안으로 들어간다.
     *
     * @param actorId 로그인한 팀원 번호
     * @param taskId  할 일 번호
     * @return 제출된 할 일
     * @throws JandiException 남의 할 일이거나, 완료하지 않았거나, 이미 제출했을 때
     */
    public Task submit(String actorId, String taskId) {
        Task t = own(actorId, taskId);
        if (t.isSubmitted()) {
            throw new JandiException("이미 제출한 할 일입니다.");
        }
        if (t.getStatus() != TaskStatus.DONE) {
            throw new JandiException("먼저 할 일 목록에서 완료 체크를 해 주세요.");
        }
        t.setStatus(TaskStatus.SUBMITTED);
        t.setSubmittedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        store.saveTasks();
        store.log(actorId, "제출", t.getId() + " " + t.getTitle());
        return t;
    }

    /**
     * 제출한 할 일을 되돌린다. 양이 박스 밖으로 나오고 상태는 '완료'로 돌아간다.
     *
     * @param actorId 로그인한 팀원 번호
     * @param taskId  할 일 번호
     * @return 되돌린 할 일
     * @throws JandiException 남의 할 일이거나 아직 제출하지 않았을 때
     */
    public Task withdraw(String actorId, String taskId) {
        Task t = own(actorId, taskId);
        if (!t.isSubmitted()) {
            throw new JandiException("아직 제출하지 않은 할 일입니다.");
        }
        t.setStatus(TaskStatus.DONE);
        t.setSubmittedAt(null);
        store.saveTasks();
        store.log(actorId, "제출 취소", t.getId() + " " + t.getTitle());
        return t;
    }

    /**
     * 메모를 고친다.
     *
     * @param actorId 로그인한 팀원 번호
     * @param taskId  할 일 번호
     * @param memo    새 메모
     * @throws JandiException 남의 할 일일 때
     */
    public void updateMemo(String actorId, String taskId, String memo) {
        Task t = own(actorId, taskId);
        t.setMemo(memo == null ? "" : memo.trim());
        store.saveTasks();
    }

    /**
     * 아직 제출하지 않은 할 일을 지운다.
     *
     * @param actorId 로그인한 팀원 번호
     * @param taskId  할 일 번호
     * @throws JandiException 남의 할 일이거나 이미 제출했을 때
     */
    public void delete(String actorId, String taskId) {
        Task t = own(actorId, taskId);
        if (t.isSubmitted()) {
            throw new JandiException("이미 제출한 할 일은 지울 수 없습니다.");
        }
        store.removeTask(taskId);
        store.log(actorId, "할 일 삭제", t.getId() + " " + t.getTitle());
    }

    private Task own(String actorId, String taskId) {
        Task t = store.findTask(taskId).orElseThrow(() -> new JandiException("할 일을 찾을 수 없습니다."));
        if (!t.getOwnerId().equals(actorId)) {
            throw new JandiException("다른 팀원의 할 일은 바꿀 수 없습니다.");
        }
        return t;
    }
}
