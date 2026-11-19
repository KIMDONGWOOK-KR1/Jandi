package jandi.service;

import jandi.domain.Commit;
import jandi.domain.Contribution;
import jandi.domain.Member;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * git 커밋으로 팀원별 기여도를 계산한다.
 *
 * <p>기여도(%) = (내 커밋 수 비율 + 내 변경 줄 수 비율) ÷ 2. 팀원과 짝지어지지 않은 작성자의 커밋은 세지 않는다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public final class ContributionService {

    private ContributionService() {
    }

    /**
     * 팀원별 기여도를 계산한다.
     *
     * @param members 팀원 목록
     * @param commits 커밋 목록
     * @return 팀원 번호별 기여도(팀원 순서 유지, 모든 팀원 포함)
     */
    public static Map<String, Contribution> compute(List<Member> members, List<Commit> commits) {
        Map<String, Contribution> map = new LinkedHashMap<>();
        members.forEach(m -> map.put(m.getId(), new Contribution(m.getId())));
        for (Commit c : commits) {
            for (Member m : members) {
                if (m.wrote(c)) {
                    map.get(m.getId()).add(c);
                    break;
                }
            }
        }
        int totalCommits = map.values().stream().mapToInt(Contribution::getCommits).sum();
        int totalLines = map.values().stream().mapToInt(Contribution::getLines).sum();
        for (Contribution c : map.values()) {
            if (totalCommits == 0) {
                c.setPercent(0);
            } else {
                double byCommits = (double) c.getCommits() / totalCommits;
                double byLines = totalLines == 0 ? byCommits : (double) c.getLines() / totalLines;
                c.setPercent((byCommits + byLines) / 2 * 100);
            }
        }
        return map;
    }
}
