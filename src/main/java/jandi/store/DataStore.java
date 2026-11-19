package jandi.store;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.domain.TaskStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 팀원, 할 일, 설정을 CSV 파일로 보관하는 저장소.
 *
 * <p>서버 없이 컴퓨터 한 대에서 동작한다. 켜질 때 전부 읽어 들이고, 바뀔 때마다 바로 저장한다.</p>
 *
 * <table class="striped">
 * <caption>저장 파일</caption>
 * <tr><th>파일</th><th>내용</th></tr>
 * <tr><td>members.csv</td><td>팀원과 git 작성자</td></tr>
 * <tr><td>tasks.csv</td><td>할 일(양)</td></tr>
 * <tr><td>settings.csv</td><td>git 저장소 경로, 일련번호</td></tr>
 * <tr><td>log.csv</td><td>작업 로그</td></tr>
 * </table>
 *
 * @author Team 3
 * @version 0.1
 */
public class DataStore {

    private static final String[] MEMBER_HEADER = {"id", "name", "gitAuthor", "color"};
    private static final String[] TASK_HEADER = {"id", "ownerId", "title", "memo", "status", "createdAt",
            "submittedAt"};
    private static final String[] SETTING_HEADER = {"key", "value"};
    private static final String[][] DEFAULT_MEMBERS = {
            {"m1", "이도훈", "7C5CFC"}, {"m2", "김영광", "F59E0B"},
            {"m3", "김동욱", "3B82F6"}, {"m4", "김경환", "EC4899"}};

    private final Path dir;
    private final List<Member> members = new ArrayList<>();
    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final Map<String, String> settings = new LinkedHashMap<>();

    private DataStore(Path dir) {
        this.dir = dir;
    }

    /**
     * 데이터 폴더를 연다. 폴더가 없으면 만들고, 팀원이 없으면 기본 팀원 4명을 넣는다.
     *
     * @param dir 데이터 폴더
     * @return 저장소
     */
    public static DataStore open(Path dir) {
        DataStore s = new DataStore(dir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        s.load();
        return s;
    }

    private void load() {
        for (String[] r : read("members.csv")) {
            if (r.length >= 4) {
                members.add(new Member(r[0], r[1], r[2], Integer.parseInt(r[3], 16)));
            }
        }
        if (members.isEmpty()) {
            for (String[] d : DEFAULT_MEMBERS) {
                members.add(new Member(d[0], d[1], d[1], Integer.parseInt(d[2], 16)));
            }
            saveMembers();
        }
        for (String[] r : read("tasks.csv")) {
            if (r.length >= 7) {
                tasks.put(r[0], new Task(r[0], r[1], r[2], r[3], TaskStatus.valueOf(r[4]),
                        LocalDateTime.parse(r[5]), r[6].isEmpty() ? null : LocalDateTime.parse(r[6])));
            }
        }
        for (String[] r : read("settings.csv")) {
            if (r.length >= 2) {
                settings.put(r[0], r[1]);
            }
        }
    }

    private List<String[]> read(String file) {
        Path p = dir.resolve(file);
        if (!Files.exists(p)) {
            return List.of();
        }
        try {
            List<String[]> rows = Csv.parse(Files.readString(p, StandardCharsets.UTF_8));
            return rows.isEmpty() ? rows : rows.subList(1, rows.size());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void write(String file, String[] header, List<String[]> rows) {
        StringBuilder sb = new StringBuilder(Csv.line(header)).append('\n');
        for (String[] r : rows) {
            sb.append(Csv.line(r)).append('\n');
        }
        try {
            Files.writeString(dir.resolve(file), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 팀원 목록을 저장한다. */
    public void saveMembers() {
        List<String[]> rows = new ArrayList<>();
        for (Member m : members) {
            rows.add(new String[]{m.getId(), m.getName(), m.getGitAuthor(), String.format("%06X", m.getColor())});
        }
        write("members.csv", MEMBER_HEADER, rows);
    }

    /** 할 일 목록을 저장한다. */
    public void saveTasks() {
        List<String[]> rows = new ArrayList<>();
        for (Task t : tasks.values()) {
            rows.add(new String[]{t.getId(), t.getOwnerId(), t.getTitle(), t.getMemo(), t.getStatus().name(),
                    t.getCreatedAt().toString(), t.getSubmittedAt() == null ? "" : t.getSubmittedAt().toString()});
        }
        write("tasks.csv", TASK_HEADER, rows);
    }

    private void saveSettings() {
        List<String[]> rows = new ArrayList<>();
        settings.forEach((k, v) -> rows.add(new String[]{k, v}));
        write("settings.csv", SETTING_HEADER, rows);
    }

    /** @return 데이터 폴더 */
    public Path dir() {
        return dir;
    }

    /** @return 팀원 목록(등록 순서) */
    public List<Member> members() {
        return members;
    }

    /**
     * @param id 팀원 번호
     * @return 팀원
     */
    public Optional<Member> findMember(String id) {
        return members.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    /** @return 모든 할 일(만든 순서) */
    public List<Task> tasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * @param id 할 일 번호
     * @return 할 일
     */
    public Optional<Task> findTask(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /**
     * 새 할 일을 만들어 저장한다.
     *
     * @param ownerId 담당 팀원 번호
     * @param title   제목
     * @param memo    메모
     * @return 만든 할 일
     */
    public Task createTask(String ownerId, String title, String memo) {
        int next = Integer.parseInt(settings.getOrDefault("nextTaskId", "1"));
        settings.put("nextTaskId", String.valueOf(next + 1));
        Task t = new Task("T" + next, ownerId, title, memo, TaskStatus.TODO,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);
        tasks.put(t.getId(), t);
        saveSettings();
        saveTasks();
        return t;
    }

    /**
     * 할 일을 지우고 저장한다.
     *
     * @param id 할 일 번호
     */
    public void removeTask(String id) {
        tasks.remove(id);
        saveTasks();
    }

    /**
     * @param key 설정 이름
     * @return 설정 값(없으면 null)
     */
    public String setting(String key) {
        return settings.get(key);
    }

    /**
     * 설정 값을 바꾸고 저장한다.
     *
     * @param key   설정 이름
     * @param value 값(null이면 지운다)
     */
    public void setSetting(String key, String value) {
        if (value == null) {
            settings.remove(key);
        } else {
            settings.put(key, value);
        }
        saveSettings();
    }

    /**
     * 작업 로그를 한 줄 남긴다.
     *
     * @param user   한 사람
     * @param action 한 일
     * @param detail 자세한 내용
     */
    public void log(String user, String action, String detail) {
        Path p = dir.resolve("log.csv");
        try {
            if (!Files.exists(p)) {
                Files.writeString(p, Csv.line("time", "user", "action", "detail") + "\n", StandardCharsets.UTF_8);
            }
            Files.writeString(p, Csv.line(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                    user, action, detail) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
