package jandi;

import jandi.domain.Member;
import jandi.service.JandiException;
import jandi.service.Ranch;
import jandi.store.DataStore;
import jandi.store.SampleData;
import jandi.ui.LoginDialog;
import jandi.ui.MainFrame;
import jandi.ui.UiKit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 잔디 프로그램의 시작점.
 *
 * <p>데이터 폴더는 기본으로 {@code <사용자 홈>/jandi-data}이다. 다음 실행 인자를 쓸 수 있다.</p>
 * <ul>
 *   <li>{@code --data <폴더>}: 데이터 폴더를 지정한다.</li>
 *   <li>{@code --repo <폴더>}: 기여도를 읽을 로컬 git 폴더를 연결한다.</li>
 *   <li>{@code --sample}: 처음 실행이면 묻지 않고 샘플 할 일과 예시 git 저장소를 만든다.</li>
 * </ul>
 *
 * @author Team 3
 * @version 0.1
 */
public final class App {

    private App() {
    }

    /**
     * 프로그램을 시작한다.
     *
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        Path dir = Paths.get(System.getProperty("user.home"), "jandi-data");
        Path repo = null;
        boolean sample = false;
        for (int i = 0; i < args.length; i++) {
            if ("--data".equals(args[i]) && i + 1 < args.length) {
                dir = Paths.get(args[++i]);
            } else if ("--repo".equals(args[i]) && i + 1 < args.length) {
                repo = Paths.get(args[++i]);
            } else if ("--sample".equals(args[i])) {
                sample = true;
            }
        }
        final Path dataDir = dir;
        final Path repoDir = repo;
        final boolean autoSample = sample;
        SwingUtilities.invokeLater(() -> start(dataDir, repoDir, autoSample));
    }

    private static void start(Path dir, Path repo, boolean autoSample) {
        UiKit.install();
        DataStore store;
        try {
            store = DataStore.open(dir);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, "데이터를 읽지 못했습니다.\n" + dir + "\n\n" + ex.getMessage(),
                    "잔디", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (store.setting("started") == null) {
            int choice = autoSample ? 0 : JOptionPane.showOptionDialog(null,
                    "처음 실행합니다. 어떻게 시작할까요?\n\n· 샘플: 할 일 11개와, 팀원 4명의 커밋이 든 예시 git 저장소를 만듭니다.\n"
                            + "· 빈 상태: 할 일 없이 시작하고, git 저장소는 화면 위 'git 저장소'에서 연결합니다.\n\n"
                            + "데이터 폴더: " + dir,
                    "잔디 시작", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new Object[]{"샘플로 시작", "빈 상태로 시작"}, "샘플로 시작");
            if (choice == 0) {
                SampleData.populateTasks(store);
                Path sampleRepo = dir.resolve("sample-repo");
                if (repo == null && SampleData.createRepo(store, sampleRepo)) {
                    store.setSetting("repoPath", sampleRepo.toAbsolutePath().toString());
                }
            }
            store.setSetting("started", "yes");
        }
        Ranch ranch = new Ranch(store);
        if (repo != null) {
            try {
                ranch.connectRepo(repo);
            } catch (JandiException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "잔디", JOptionPane.WARNING_MESSAGE);
            }
        }
        showLogin(ranch);
    }

    private static void showLogin(Ranch ranch) {
        LoginDialog login = new LoginDialog(null, ranch);
        login.setVisible(true);
        Member me = login.getMember();
        if (me == null) {
            System.exit(0);
            return;
        }
        ranch.store().log(me.getId(), "로그인", me.getName());
        ranch.refreshGit();
        new MainFrame(ranch, me, () -> SwingUtilities.invokeLater(() -> showLogin(ranch))).setVisible(true);
    }
}
