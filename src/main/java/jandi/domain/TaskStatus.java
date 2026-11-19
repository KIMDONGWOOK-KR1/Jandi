package jandi.domain;

/**
 * 할 일(양)의 상태.
 *
 * @author Team 3
 * @version 0.1
 */
public enum TaskStatus {
    /** 아직 하지 않은 일. 양이 박스 밖에 있다. */
    TODO("할 일"),
    /** 완료 체크는 했지만 아직 제출하지 않은 일. */
    DONE("완료"),
    /** 제출해서 양이 박스 안으로 들어간 일. */
    SUBMITTED("제출됨");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    /** @return 화면에 보여 줄 이름 */
    public String label() {
        return label;
    }
}
