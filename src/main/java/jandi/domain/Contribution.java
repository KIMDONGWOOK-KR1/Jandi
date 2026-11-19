package jandi.domain;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * 팀원 한 명의 git 기여도 집계.
 *
 * @author Team 3
 * @version 0.1
 */
public class Contribution {

    private final String memberId;
    private int commits;
    private int added;
    private int deleted;
    private double percent;
    private final Map<LocalDate, Integer> daily = new TreeMap<>();

    /** @param memberId 팀원 번호 */
    public Contribution(String memberId) {
        this.memberId = memberId;
    }

    /**
     * 커밋 한 건을 더한다.
     *
     * @param c 커밋
     */
    public void add(Commit c) {
        commits++;
        added += c.added();
        deleted += c.deleted();
        daily.merge(c.time().toLocalDate(), 1, Integer::sum);
    }

    /** @return 팀원 번호 */
    public String getMemberId() {
        return memberId;
    }

    /** @return 커밋 수 */
    public int getCommits() {
        return commits;
    }

    /** @return 추가한 줄 수 */
    public int getAdded() {
        return added;
    }

    /** @return 지운 줄 수 */
    public int getDeleted() {
        return deleted;
    }

    /** @return 바꾼 줄 수(추가 + 삭제) */
    public int getLines() {
        return added + deleted;
    }

    /** @return 기여도(0~100) */
    public double getPercent() {
        return percent;
    }

    /** @param percent 기여도(0~100) */
    public void setPercent(double percent) {
        this.percent = percent;
    }

    /** @return 날짜별 커밋 수 */
    public Map<LocalDate, Integer> getDaily() {
        return daily;
    }
}
