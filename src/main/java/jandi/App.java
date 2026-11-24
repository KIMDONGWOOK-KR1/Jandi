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
 * <p>시작 순서: 실행 인자 읽기 → 데이터 폴더 열기 → 처음 실행이면 샘플 여부 묻기 → {@link Ranch} 만들기
 * → 로그인 창 → 메인 창. 화면에 관한 일은 모두 Swing의 이벤트 처리 스레드에서 한다.</p>
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

    /** 객체를 만들 일이 없는 클래스라서 생성자를 막아 둔다. */
    private App() {
    }

    /**
     * 프로그램을 시작한다.
     *
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        // Swing 컴포넌트는 이벤트 처리 스레드(EDT) 하나에서만 만들고 바꿔야 안전하다.
        // 그래서 main 스레드에서 바로 창을 만들지 않고, 시작 작업을 이벤트 큐에 넣어 그 스레드가 실행하게 한다.
        SwingUtilities.invokeLater(() -> start(args));
    }

    /** 실행 인자를 읽고, 데이터를 열고, 로그인 창까지 띄운다. 이벤트 처리 스레드에서 실행된다. */
    private static void start(String[] args) {
        // 1) 실행 인자 읽기. 값이 따라오는 인자(--data, --repo)는 ++i로 다음 칸을 값으로 읽고 건너뛴다.
        Path dir = Paths.get(System.getProperty("user.home"), "jandi-data");
        Path repo = null;
        boolean autoSample = false;
        for (int i = 0; i < args.length; i++) {
            if ("--data".equals(args[i]) && i + 1 < args.length) {
                dir = Paths.get(args[++i]);
            } else if ("--repo".equals(args[i]) && i + 1 < args.length) {
                repo = Paths.get(args[++i]);
            } else if ("--sample".equals(args[i])) {
                autoSample = true;
            }
        }
        // 2) 글꼴 정하기. 기본 글꼴은 그 뒤에 만들어지는 컴포넌트에 적용되므로 창을 띄우기 전에 부른다.
        UiKit.install();
        // 3) 데이터 폴더 열기. 읽지 못하면 이유를 알리고 더 진행하지 않는다.
        DataStore store;
        try {
            store = DataStore.open(dir);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, "데이터를 읽지 못했습니다.\n" + dir + "\n\n" + ex.getMessage(),
                    "잔디", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 4) 처음 실행인지는 설정 값 "started"가 있는지로 안다. 한 번 고르면 다음 실행부터는 묻지 않는다.
        if (store.setting("started") == null) {
            // showOptionDialog는 고른 버튼의 순서를 돌려준다(0 = 샘플). 창을 그냥 닫으면 -1이라 빈 상태로 시작한다.
            int choice = autoSample ? 0 : JOptionPane.showOptionDialog(null,
                    "처음 실행합니다. 어떻게 시작할까요?\n\n· 샘플: 할 일 11개와, 팀원 4명의 커밋이 든 예시 git 저장소를 만듭니다.\n"
                            + "· 빈 상태: 할 일 없이 시작하고, git 저장소는 화면 위 'git 저장소'에서 연결합니다.\n\n"
                            + "데이터 폴더: " + dir,
                    "잔디 시작", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new Object[]{"샘플로 시작", "빈 상태로 시작"}, "샘플로 시작");
            if (choice == 0) {
                SampleData.populateTasks(store);
                Path sampleRepo = dir.resolve("sample-repo");
                // --repo로 저장소를 직접 준 경우에는 예시 저장소를 만들지 않는다.
                if (repo == null && SampleData.createRepo(store, sampleRepo)) {
                    store.setSetting("repoPath", sampleRepo.toAbsolutePath().toString());
                }
            }
            store.setSetting("started", "yes");
        }
        // 5) 서비스 창구(Ranch) 만들기. 화면은 이 객체 하나를 통해 할 일과 기여도 기능을 쓴다.
        Ranch ranch = new Ranch(store);
        // 6) --repo가 있으면 그 저장소를 연결한다. git 저장소가 아니면 경고만 하고 계속 진행한다.
        if (repo != null) {
            try {
                ranch.connectRepo(repo);
            } catch (JandiException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "잔디", JOptionPane.WARNING_MESSAGE);
            }
        }
        // 7) 로그인 창 띄우기
        showLogin(ranch);
    }

    /** 로그인 창을 띄우고, 팀원을 고르면 메인 창을 연다. 로그아웃하면 다시 불린다. */
    private static void showLogin(Ranch ranch) {
        LoginDialog login = new LoginDialog(null, ranch);
        // 모달 대화상자라서 setVisible(true)는 창이 닫힐 때까지 돌아오지 않는다. 닫힌 뒤에 고른 팀원을 꺼낸다.
        login.setVisible(true);
        Member me = login.getMember();
        if (me == null) { // 아무도 고르지 않고 창을 닫았으면 프로그램을 끝낸다.
            System.exit(0);
            return;
        }
        ranch.store().log(me.getId(), "로그인", me.getName());
        ranch.refreshGit(); // 로그인할 때마다 커밋을 새로 읽어 기여도를 최신으로 맞춘다.
        // 세 번째 인자는 로그아웃했을 때 부를 동작(콜백)이다. 메인 창은 App을 모르고 이 Runnable만 실행한다.
        // invokeLater로 감싸, 로그아웃 버튼의 이벤트 처리가 끝난 다음에 로그인 창이 뜨게 한다.
        new MainFrame(ranch, me, () -> SwingUtilities.invokeLater(() -> showLogin(ranch))).setVisible(true);
    }
}
