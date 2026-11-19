package jandi.domain;

/**
 * 팀원.
 *
 * <p>{@code gitAuthor}는 이 팀원이 git 커밋에 남기는 작성자 이름 또는 이메일이다.
 * 여러 개를 쓰면 {@code ;}로 구분한다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public class Member {

    private final String id;
    private final String name;
    private String gitAuthor;
    private final int color;

    /**
     * @param id        팀원 번호
     * @param name      이름
     * @param gitAuthor git 작성자 이름 또는 이메일
     * @param color     대표 색(RGB)
     */
    public Member(String id, String name, String gitAuthor, int color) {
        this.id = id;
        this.name = name;
        this.gitAuthor = gitAuthor == null ? "" : gitAuthor;
        this.color = color;
    }

    /** @return 팀원 번호 */
    public String getId() {
        return id;
    }

    /** @return 이름 */
    public String getName() {
        return name;
    }

    /** @return git 작성자 이름 또는 이메일 */
    public String getGitAuthor() {
        return gitAuthor;
    }

    /** @param gitAuthor git 작성자 이름 또는 이메일 */
    public void setGitAuthor(String gitAuthor) {
        this.gitAuthor = gitAuthor == null ? "" : gitAuthor.trim();
    }

    /** @return 대표 색(RGB) */
    public int getColor() {
        return color;
    }

    /**
     * 커밋이 이 팀원의 것인지 확인한다. 작성자 이름이나 이메일이 같으면(대소문자 무시) 본인 커밋이다.
     *
     * @param c 커밋
     * @return 본인 커밋이면 true
     */
    public boolean wrote(Commit c) {
        for (String a : gitAuthor.split(";")) {
            String key = a.trim();
            if (!key.isEmpty() && (key.equalsIgnoreCase(c.authorName()) || key.equalsIgnoreCase(c.authorEmail()))) {
                return true;
            }
        }
        return false;
    }
}
