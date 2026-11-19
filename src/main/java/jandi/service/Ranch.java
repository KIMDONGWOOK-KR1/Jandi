package jandi.service;

import jandi.domain.Commit;
import jandi.domain.Contribution;
import jandi.domain.Member;
import jandi.store.DataStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 목장. 저장소, 할 일 서비스, git 기여도를 한데 묶어 화면에 내주는 창구이다.
 *
 * @author Team 3
 * @version 0.1
 */
public class Ranch {

    private final DataStore store;
    private final TaskService tasks;
    private final GitService git = new GitService();
    private List<Commit> commits = List.of();
    private Map<String, Contribution> contributions;
    private boolean gitReady;
    private String gitMessage = "";

    /** @param store 저장소 */
    public Ranch(DataStore store) {
        this.store = store;
        this.tasks = new TaskService(store);
        refreshGit();
    }

    /** @return 저장소 */
    public DataStore store() {
        return store;
    }

    /** @return 할 일 서비스 */
    public TaskService tasks() {
        return tasks;
    }

    /** @return 팀원 목록 */
    public List<Member> members() {
        return store.members();
    }

    /** @return 연결된 git 저장소 폴더(없으면 null) */
    public Path repoPath() {
        String p = store.setting("repoPath");
        return p == null || p.isBlank() ? null : Paths.get(p);
    }

    /**
     * git 저장소 폴더를 연결하고 다시 읽는다.
     *
     * @param repo git 저장소 폴더
     * @throws JandiException 폴더가 git 저장소가 아닐 때
     */
    public void connectRepo(Path repo) {
        if (!git.isRepo(repo)) {
            throw new JandiException("git 저장소가 아닙니다.\n" + repo);
        }
        store.setSetting("repoPath", repo.toAbsolutePath().toString());
        store.log("-", "저장소 연결", repo.toString());
        refreshGit();
    }

    /**
     * 팀원의 git 작성자를 정하고 기여도를 다시 계산한다.
     *
     * @param memberId  팀원 번호
     * @param gitAuthor git 작성자 이름 또는 이메일
     */
    public void setGitAuthor(String memberId, String gitAuthor) {
        store.findMember(memberId).ifPresent(m -> m.setGitAuthor(gitAuthor));
        store.saveMembers();
        contributions = ContributionService.compute(members(), commits);
    }

    /** 연결된 git 저장소를 다시 읽고 기여도를 다시 계산한다. */
    public final void refreshGit() {
        Path repo = repoPath();
        commits = List.of();
        gitReady = false;
        if (repo == null) {
            gitMessage = "git 저장소가 연결되지 않았습니다.";
        } else {
            try {
                commits = git.readLog(repo);
                gitReady = true;
                gitMessage = repo + " · 커밋 " + commits.size() + "개";
            } catch (JandiException e) {
                gitMessage = e.getMessage();
            }
        }
        contributions = ContributionService.compute(members(), commits);
    }

    /** @return git 저장소를 읽는 데 성공했으면 true */
    public boolean isGitReady() {
        return gitReady;
    }

    /** @return git 연결 상태 안내 */
    public String gitMessage() {
        return gitMessage;
    }

    /** @return 읽은 커밋 목록 */
    public List<Commit> commits() {
        return commits;
    }

    /** @return 팀원 번호별 기여도 */
    public Map<String, Contribution> contributions() {
        return contributions;
    }

    /**
     * @param memberId 팀원 번호
     * @return 그 팀원의 기여도
     */
    public Contribution contributionOf(String memberId) {
        return contributions.get(memberId);
    }
}
