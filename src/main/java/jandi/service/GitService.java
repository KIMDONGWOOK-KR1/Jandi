package jandi.service;

import jandi.domain.Commit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 로컬 git 폴더를 읽는다. 컴퓨터에 설치된 {@code git} 명령을 실행하며 네트워크는 쓰지 않는다.
 *
 * @author Team 3
 * @version 0.1
 */
public class GitService {

    /** 커밋 구분 문자. */
    private static final String RS = "\u001e";
    /** 항목 구분 문자. */
    private static final String US = "\u001f";

    /**
     * git 명령 실행 결과.
     *
     * @param exit 종료 코드
     * @param out  표준 출력
     */
    public record Result(int exit, String out) {
    }

    /**
     * git 명령을 실행한다.
     *
     * @param cwd  실행할 폴더
     * @param env  더할 환경 변수(없으면 null)
     * @param args git 뒤에 붙일 인자
     * @return 실행 결과
     */
    public static Result exec(Path cwd, Map<String, String> env, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(cwd.toFile());
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        if (env != null) {
            pb.environment().putAll(env);
        }
        try {
            Process p = pb.start();
            p.getOutputStream().close();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new Result(p.waitFor(), out);
        } catch (IOException e) {
            throw new JandiException("git을 실행하지 못했습니다. git이 설치되어 있는지 확인해 주세요.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JandiException("git 실행이 중단되었습니다.");
        }
    }

    /**
     * @param dir 폴더
     * @return git 저장소(작업 폴더)이면 true
     * @throws JandiException git을 실행할 수 없을 때(설치되지 않음 등)
     */
    public boolean isRepo(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        Result r = exec(dir, null, "rev-parse", "--is-inside-work-tree");
        return r.exit() == 0 && r.out().trim().equals("true");
    }

    /**
     * 저장소의 내 브랜치와 원격 브랜치에서 병합 커밋을 뺀 커밋을 읽는다.
     *
     * @param repo git 저장소 폴더
     * @return 커밋 목록(최신순). 커밋이 없는 저장소면 빈 목록
     * @throws JandiException 폴더가 git 저장소가 아니거나 git을 실행할 수 없을 때
     */
    public List<Commit> readLog(Path repo) {
        if (!isRepo(repo)) {
            throw new JandiException("git 저장소가 아닙니다: " + repo);
        }
        Result r = exec(repo, null, "log", "--branches", "--remotes", "--no-merges", "--numstat",
                "--pretty=format:%x1e%an%x1f%ae%x1f%aI");
        return r.exit() == 0 ? parse(r.out()) : List.of();
    }

    /**
     * {@code git log --numstat} 출력을 커밋 목록으로 바꾼다.
     *
     * @param output git 출력
     * @return 커밋 목록
     */
    public static List<Commit> parse(String output) {
        List<Commit> list = new ArrayList<>();
        for (String chunk : output.split(RS)) {
            if (chunk.isBlank()) {
                continue;
            }
            String[] lines = chunk.split("\n");
            String[] h = lines[0].split(US);
            if (h.length < 3) {
                continue;
            }
            int added = 0;
            int deleted = 0;
            for (int i = 1; i < lines.length; i++) {
                String[] n = lines[i].split("\t");
                if (n.length >= 3) {
                    added += num(n[0]);
                    deleted += num(n[1]);
                }
            }
            list.add(new Commit(h[0], h[1],
                    OffsetDateTime.parse(h[2].trim()).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
                    added, deleted));
        }
        return list;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0; // 이진 파일은 "-"로 나온다.
        }
    }

    /**
     * @param commits 커밋 목록
     * @return 커밋에 나온 작성자 이름들(가나다순)
     */
    public static List<String> authors(List<Commit> commits) {
        TreeSet<String> set = new TreeSet<>();
        for (Commit c : commits) {
            set.add(c.authorName());
        }
        return new ArrayList<>(set);
    }
}
