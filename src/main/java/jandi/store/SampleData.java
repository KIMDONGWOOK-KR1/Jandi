package jandi.store;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.domain.TaskStatus;
import jandi.service.GitService;
import jandi.service.JandiException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 시연용 샘플. 할 일 몇 개와, 팀원 4명의 커밋이 든 예시 git 저장소를 만든다.
 *
 * @author Team 3
 * @version 0.1
 */
public final class SampleData {

    /** {팀원 순번, 제목, 메모, 상태} */
    private static final String[][] TASKS = {
            {"0", "요구사항 정리", "유스케이스 8개로 정리", "SUBMITTED"},
            {"0", "기능 명세", "함수 명세 6개 작성", "SUBMITTED"},
            {"0", "발표 자료", "체크포인트 발표 슬라이드", "SUBMITTED"},
            {"1", "로그인 화면", "이름 선택 로그인", "SUBMITTED"},
            {"1", "목장 화면", "박스와 양 그리기", "SUBMITTED"},
            {"2", "저장 기능", "CSV로 저장/불러오기", "SUBMITTED"},
            {"2", "기여도 계산", "커밋 수와 줄 수로 계산", "SUBMITTED"},
            {"2", "클래스 다이어그램", "도메인 클래스 정리", "DONE"},
            {"3", "색상 정리", "팀원별 대표 색", "SUBMITTED"},
            {"3", "상세 패널", "", "TODO"},
            {"3", "테스트 작성", "", "TODO"}};

    /** 팀원 순번별 커밋 수. */
    private static final int[] COMMITS = {13, 9, 7, 3};
    private static final String[] AREAS = {"docs/plan", "ui/ranch", "store/data", "ui/color"};

    private SampleData() {
    }

    /**
     * 샘플 할 일을 넣는다.
     *
     * @param store 저장소
     */
    public static void populateTasks(DataStore store) {
        List<Member> ms = store.members();
        for (String[] t : TASKS) {
            Task task = store.createTask(ms.get(Integer.parseInt(t[0])).getId(), t[1], t[2]);
            task.setStatus(TaskStatus.valueOf(t[3]));
            if (task.isSubmitted()) {
                task.setSubmittedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            }
        }
        store.saveTasks();
    }

    /**
     * 팀원 4명의 커밋이 든 예시 git 저장소를 만든다.
     *
     * @param store 저장소(팀원 이름을 git 작성자로 쓴다)
     * @param repo  만들 폴더
     * @return 만들었으면 true, git을 쓸 수 없으면 false
     */
    public static boolean createRepo(DataStore store, Path repo) {
        try {
            Files.createDirectories(repo);
            if (GitService.exec(repo, null, "init", "-q").exit() != 0) {
                return false;
            }
            List<Member> ms = store.members();
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            int seq = 0;
            // 오래된 날부터 차례로 커밋을 쌓는다.
            for (int day = 44; day >= 0; day--) {
                for (int m = 0; m < ms.size(); m++) {
                    int every = Math.max(1, 45 / COMMITS[m]);
                    if ((day + m * 2) % every != 0 || day / every >= COMMITS[m]) {
                        continue;
                    }
                    seq++;
                    Path file = repo.resolve(AREAS[m] + (seq % 3) + ".txt");
                    Files.createDirectories(file.getParent());
                    StringBuilder sb = new StringBuilder();
                    int lines = 6 + (seq * 7 + m * 11) % 30;
                    for (int i = 0; i < lines; i++) {
                        sb.append("작업 ").append(seq).append(" 줄 ").append(i).append('\n');
                    }
                    Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    String when = now.minusDays(day).minusHours(m).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    String name = ms.get(m).getName();
                    GitService.exec(repo, null, "add", "-A");
                    GitService.exec(repo, Map.of("GIT_AUTHOR_DATE", when, "GIT_COMMITTER_DATE", when),
                            "-c", "user.name=" + name, "-c", "user.email=" + ms.get(m).getId() + "@jandi.local",
                            "-c", "commit.gpgsign=false", "commit", "-q", "-m", AREAS[m] + " 작업 " + seq);
                }
            }
            return true;
        } catch (JandiException e) { // git이 없거나 실행하지 못함
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
