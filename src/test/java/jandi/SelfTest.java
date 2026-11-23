package jandi;

import jandi.domain.Commit;
import jandi.domain.Contribution;
import jandi.domain.Member;
import jandi.domain.Task;
import jandi.domain.TaskStatus;
import jandi.service.ContributionService;
import jandi.service.GitService;
import jandi.service.JandiException;
import jandi.service.Ranch;
import jandi.store.Csv;
import jandi.store.DataStore;
import jandi.store.SampleData;
import jandi.ui.GrassPanel;
import jandi.ui.RanchPanel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 외부 라이브러리 없이 돌리는 자체 테스트. 하나라도 실패하면 종료 코드 1로 끝난다.
 */
public final class SelfTest {

    private static int passed;
    private static int failed;

    private SelfTest() {
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("  통과  " + name);
        } catch (Throwable e) {
            failed++;
            System.out.println("  실패  " + name + " → " + e);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private static void denied(Body body, String what) throws Exception {
        try {
            body.run();
        } catch (JandiException e) {
            return;
        }
        throw new AssertionError("막혀야 하는데 통과함: " + what);
    }

    private static Commit commit(String author, int daysAgo, int added, int deleted) {
        return new Commit(author, author + "@x", LocalDateTime.now().minusDays(daysAgo), added, deleted);
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("jandi-test");

        test("CSV: 쉼표·따옴표·줄바꿈이 든 값을 그대로 되읽는다", () -> {
            String line = Csv.line("a,b", "말 \"따옴표\"", "두 줄\n메모", "");
            List<String[]> rows = Csv.parse("h1,h2,h3,h4\n" + line + "\n");
            check(rows.size() == 2, "줄 수");
            check(rows.get(1)[0].equals("a,b") && rows.get(1)[1].equals("말 \"따옴표\"")
                    && rows.get(1)[2].equals("두 줄\n메모") && rows.get(1)[3].isEmpty(), "값");
        });

        test("저장소: 처음 열면 팀원 4명이 들어 있다", () -> {
            DataStore s = DataStore.open(tmp.resolve("a"));
            check(s.members().size() == 4, "4명");
            check(s.members().get(0).getName().equals("이도훈"), "첫 팀원");
        });

        test("할 일: 추가 → 완료 → 제출 순서로 양이 박스에 들어간다", () -> {
            Ranch r = new Ranch(DataStore.open(tmp.resolve("b")));
            Task t = r.tasks().add("m1", "보고서", "");
            check(t.getStatus() == TaskStatus.TODO, "처음은 할 일");
            denied(() -> r.tasks().submit("m1", t.getId()), "완료 전 제출");
            r.tasks().toggleDone("m1", t.getId());
            check(t.getStatus() == TaskStatus.DONE, "완료");
            r.tasks().submit("m1", t.getId());
            check(t.isSubmitted() && t.getSubmittedAt() != null, "제출됨");
            denied(() -> r.tasks().toggleDone("m1", t.getId()), "제출 후 체크 변경");
            denied(() -> r.tasks().delete("m1", t.getId()), "제출 후 삭제");
        });

        test("할 일: 제출한 양을 내보내면 완료 상태로 돌아간다", () -> {
            Ranch r = new Ranch(DataStore.open(tmp.resolve("b2")));
            Task t = r.tasks().add("m1", "보고서", "");
            r.tasks().toggleDone("m1", t.getId());
            denied(() -> r.tasks().withdraw("m1", t.getId()), "제출 전 내보내기");
            r.tasks().submit("m1", t.getId());
            denied(() -> r.tasks().withdraw("m2", t.getId()), "남의 양 내보내기");
            r.tasks().withdraw("m1", t.getId());
            check(t.getStatus() == TaskStatus.DONE && t.getSubmittedAt() == null, "완료로 돌아감");
            r.tasks().submit("m1", t.getId());
            check(t.isSubmitted(), "다시 제출");
        });

        test("할 일: 남의 할 일은 바꿀 수 없고, 빈 제목은 안 된다", () -> {
            Ranch r = new Ranch(DataStore.open(tmp.resolve("c")));
            Task t = r.tasks().add("m1", "내 일", "");
            denied(() -> r.tasks().toggleDone("m2", t.getId()), "남의 완료 체크");
            denied(() -> r.tasks().updateMemo("m2", t.getId(), "x"), "남의 메모");
            denied(() -> r.tasks().add("m1", "  ", ""), "빈 제목");
            check(r.tasks().tasksOf("m2").isEmpty(), "m2는 할 일이 없다");
        });

        test("할 일: 저장한 뒤 다시 열어도 그대로다", () -> {
            Path d = tmp.resolve("d");
            Ranch r = new Ranch(DataStore.open(d));
            Task t = r.tasks().add("m3", "저장, 확인", "첫 줄\n둘째 줄");
            r.tasks().toggleDone("m3", t.getId());
            Task again = DataStore.open(d).findTask(t.getId()).orElseThrow();
            check(again.getTitle().equals("저장, 확인") && again.getMemo().equals("첫 줄\n둘째 줄")
                    && again.getStatus() == TaskStatus.DONE, "되읽기");
            check(DataStore.open(d).createTask("m3", "다음", "").getId().equals("T2"), "일련번호 이어짐");
        });

        test("git: log 출력을 커밋으로 읽는다", () -> {
            String out = "\u001e이도훈\u001fa@b.c\u001f2026-11-01T10:00:00+09:00\n"
                    + "3\t1\tsrc/A.java\n-\t-\timg.png\n\n"
                    + "\u001e김영광\u001fk@b.c\u001f2026-11-02T11:00:00+09:00\n10\t0\tB.java\n";
            List<Commit> cs = GitService.parse(out);
            check(cs.size() == 2, "2건");
            check(cs.get(0).added() == 3 && cs.get(0).deleted() == 1 && cs.get(0).authorEmail().equals("a@b.c"), "첫 건");
            check(cs.get(1).authorName().equals("김영광") && cs.get(1).lines() == 10, "둘째 건");
        });

        test("기여도: (커밋 비율 + 줄 수 비율) ÷ 2, 합은 100%", () -> {
            List<Member> ms = List.of(new Member("m1", "가", "가", 0), new Member("m2", "나", "na@x", 0),
                    new Member("m3", "다", "다", 0));
            List<Commit> cs = List.of(commit("가", 1, 30, 0), commit("가", 1, 30, 0), commit("가", 2, 0, 0),
                    commit("NA", 3, 20, 20), commit("모르는사람", 3, 999, 0));
            Map<String, Contribution> m = ContributionService.compute(ms, cs);
            // 가: 커밋 3/4, 줄 60/100 → 67.5 / 나(이메일로 짝지음): 커밋 1/4, 줄 40/100 → 32.5
            check(Math.abs(m.get("m1").getPercent() - 67.5) < 1e-9, "가 " + m.get("m1").getPercent());
            check(Math.abs(m.get("m2").getPercent() - 32.5) < 1e-9, "나 " + m.get("m2").getPercent());
            check(m.get("m3").getPercent() == 0 && m.get("m3").getCommits() == 0, "다");
            check(m.get("m1").getDaily().size() == 2, "날짜별 집계");
        });

        test("기여도: 커밋이 없으면 모두 0%", () -> {
            Map<String, Contribution> m = ContributionService.compute(
                    List.of(new Member("m1", "가", "가", 0)), List.of());
            check(m.get("m1").getPercent() == 0, "0%");
        });

        test("화면 계산: 박스 넓이는 기여도에 비례하고, 잔디 단계는 0~4", () -> {
            check(RanchPanel.penScale(40, 40) == 1.0, "최고 기여자는 1");
            check(Math.abs(RanchPanel.penScale(10, 40) - 0.5) < 1e-9, "1/4 기여 → 변 1/2");
            check(RanchPanel.penScale(0, 40) == 0.42, "최소 크기");
            check(GrassPanel.level(0, 5) == 0 && GrassPanel.level(1, 5) == 1 && GrassPanel.level(5, 5) == 4, "단계");
        });

        test("git: 저장소가 아닌 폴더는 연결을 거절한다", () -> {
            Ranch r = new Ranch(DataStore.open(tmp.resolve("e")));
            check(!r.isGitReady(), "처음엔 미연결");
            denied(() -> r.connectRepo(Files.createDirectories(tmp.resolve("not-a-repo"))), "git 아닌 폴더");
        });

        test("git: 실제 로컬 저장소를 읽어 4명의 기여도를 만든다", () -> {
            DataStore s = DataStore.open(tmp.resolve("f"));
            Path repo = tmp.resolve("f-repo");
            check(SampleData.createRepo(s, repo), "예시 저장소 생성(git 필요)");
            Ranch r = new Ranch(s);
            r.connectRepo(repo);
            check(r.isGitReady() && r.commits().size() == 32, "커밋 수 " + r.commits().size());
            double sum = 0;
            for (Member m : r.members()) {
                check(r.contributionOf(m.getId()).getCommits() > 0, m.getName() + " 커밋 있음");
                sum += r.contributionOf(m.getId()).getPercent();
            }
            check(Math.abs(sum - 100) < 1e-6, "합 100%");
            check(r.contributionOf("m1").getPercent() > r.contributionOf("m4").getPercent(), "많이 한 사람이 더 큼");
            r.setGitAuthor("m4", "없는사람");
            check(r.contributionOf("m4").getCommits() == 0, "작성자를 바꾸면 다시 계산");
        });

        System.out.println();
        System.out.println("자체 테스트: " + passed + "개 통과, " + failed + "개 실패");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
