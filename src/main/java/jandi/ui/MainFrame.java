package jandi.ui;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.JandiException;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 메인 창. 왼쪽에 내 정보와 할 일, 가운데에 목장/양털, 오른쪽에 상세, 아래에 팀원 줄이 있다.
 *
 * <p>패널들은 데이터를 직접 바꾸지 않는다. 무엇이 눌렸는지를 콜백으로 이 창에 알리면, 이 창이 {@link Ranch}를 통해
 * 서비스를 부르고({@code act}) 모든 패널을 다시 그린다({@link #refresh()}). MVC로 보면 패널이 뷰,
 * 이 창이 컨트롤러, 서비스와 도메인이 모델에 해당한다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public class MainFrame extends JFrame {

    private final Ranch ranch;
    private final Member me;
    private final RanchPanel ranchPanel;
    private final GrassPanel grassPanel;
    private final TeamStrip strip;
    private final TaskListPanel taskList;
    private final DetailPanel detail;
    /** 프로필 카드의 "양 N마리 · 기여도 N%" 한 줄. refresh()에서 고친다. */
    private final JLabel profileSub = UiKit.label("", Font.PLAIN, 11, UiKit.MUTED);
    /** 지금 고른 할 일 번호(없으면 null). 모든 패널이 이 값 하나를 기준으로 선택 표시를 한다. */
    private String selectedId;

    /**
     * 패널을 만들어 BorderLayout의 네 영역에 배치하고 이벤트를 연결한다.
     *
     * @param ranch    목장
     * @param me       로그인한 팀원
     * @param onLogout 로그아웃했을 때 부를 동작
     */
    public MainFrame(Ranch ranch, Member me, Runnable onLogout) {
        super("잔디 — 팀 기여도 목장");
        this.ranch = ranch;
        this.me = me;
        setDefaultCloseOperation(EXIT_ON_CLOSE); // 창을 닫으면 프로그램도 끝난다.

        // 바탕 패널. BorderLayout(가로 간격 18, 세로 간격 12)의 서·가운데·동·남 영역에 아래 패널들을 붙인다.
        JPanel root = new UiKit.Meadow(new BorderLayout(18, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        // 패널 만들기. 각 패널에 "눌렸을 때 부를 동작"을 메서드 참조(this::select 등)로 넘긴다.
        // 패널은 그 동작을 부르기만 하고(콜백), 실제 처리는 이 클래스의 메서드가 한다.
        ranchPanel = new RanchPanel(ranch, me, this::select);
        grassPanel = new GrassPanel(ranch);
        strip = new TeamStrip(ranch, this::select);
        detail = new DetailPanel(ranch, me, this::submit, this::delete, this::withdraw);
        // 할 일 목록은 알릴 동작이 세 가지라서 인터페이스(Actions)로 받는다. 여기서 익명 클래스로 구현한다.
        taskList = new TaskListPanel(ranch, me, new TaskListPanel.Actions() {
            @Override
            public void add() {
                addTask();
            }

            @Override
            public void toggle(Task t) {
                act(() -> ranch.tasks().toggleDone(me.getId(), t.getId()), t.getId());
            }

            @Override
            public void memo(Task t) {
                editMemo(t);
            }
        }, this::select);

        // 왼쪽: 내 정보 + 내 할 일
        JPanel left = UiKit.clear(new BorderLayout(0, 12));
        left.setPreferredSize(new Dimension(290, 100)); // WEST 영역은 너비(290)만 쓰이고, 높이는 창에 맞춰 늘어난다.
        left.add(profileCard(onLogout), BorderLayout.NORTH);
        left.add(taskList, BorderLayout.CENTER);

        // 가운데: 탭 + 목장/양털
        // CardLayout은 같은 자리에 패널을 겹쳐 두고, 이름("ranch", "grass")으로 한 장만 보여 준다.
        CardLayout cards = new CardLayout();
        JPanel center = UiKit.clear(cards);
        center.add(ranchPanel, "ranch");
        center.add(grassPanel, "grass");
        UiKit.RoundButton tabRanch = new UiKit.RoundButton("목장", true);
        UiKit.RoundButton tabGrass = new UiKit.RoundButton("양털", false);
        UiKit.RoundButton repo = new UiKit.RoundButton("git 저장소", false);
        ActionListener tab = e -> { // 두 탭 버튼이 같은 리스너를 쓰고, 눌린 버튼(이벤트 소스)으로 구분한다.
            boolean grass = e.getSource() == tabGrass;
            cards.show(center, grass ? "grass" : "ranch");
            tabRanch.setPrimary(!grass);
            tabGrass.setPrimary(grass);
        };
        tabRanch.addActionListener(tab);
        tabGrass.addActionListener(tab);
        repo.addActionListener(e -> {
            new RepoDialog(this, ranch).setVisible(true); // 모달이라 그 창을 닫아야 다음 줄로 넘어간다.
            refresh(); // 저장소나 git 작성자가 바뀌었을 수 있으니 다시 그린다.
        });
        // 탭 버튼 세 개를 카드 한 장에 가로로 놓는다(FlowLayout).
        UiKit.Card tabs = new UiKit.Card();
        tabs.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        tabs.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));
        tabs.add(tabRanch);
        tabs.add(tabGrass);
        tabs.add(repo);
        // NORTH 영역에 바로 넣으면 카드가 가로로 끝까지 늘어나므로, FlowLayout 패널로 한 번 감싸 제 크기로 가운데에 둔다.
        JPanel tabRow = UiKit.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabRow.add(tabs);
        JPanel mid = UiKit.clear(new BorderLayout(0, 8));
        mid.add(tabRow, BorderLayout.NORTH);
        mid.add(center, BorderLayout.CENTER);

        // 오른쪽: 상세
        JPanel right = UiKit.clear(new BorderLayout());
        right.add(detail, BorderLayout.NORTH); // NORTH에 넣어, 상세 카드가 세로로 늘어나지 않고 제 높이를 지키게 한다.

        // 네 영역에 붙인다. 창 크기를 바꾸면 남는 공간은 가운데(CENTER)가 차지한다.
        root.add(left, BorderLayout.WEST);
        root.add(mid, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        root.add(strip, BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1400, 880);
        setLocationRelativeTo(null); // 화면 가운데에 띄운다.
        // WindowAdapter는 WindowListener의 메서드를 모두 빈 채로 구현해 둔 클래스라, 필요한 것 하나만 오버라이드하면 된다.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                // 창이 다시 활성화되면(다른 프로그램에서 커밋하고 돌아왔을 때 등) 새 커밋을 다시 읽는다.
                // 알려진 한계: 대화상자를 닫아 이 창이 다시 활성화될 때도 불려서, 그때마다 git log를 다시 읽는다.
                ranch.refreshGit();
                refresh();
            }
        });
        refresh(); // 처음 한 번 그린다.
    }

    /** 왼쪽 위 프로필 카드: 색 동그라미(이름 첫 글자) + 이름 + 양·기여도 한 줄 + 로그아웃 버튼. */
    private UiKit.Card profileCard(Runnable onLogout) {
        UiKit.Card card = new UiKit.Card();
        card.setLayout(new BorderLayout(10, 0));
        Color mine = new Color(me.getColor());
        // 동그라미는 클래스를 따로 만들지 않고, JPanel의 paintComponent를 익명 클래스로 오버라이드해 직접 그린다.
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = UiKit.aa(g0.create()); // 복사본에 그려서 원래 Graphics의 설정을 바꾸지 않는다.
                g.setColor(mine);
                g.fillOval(1, (getHeight() - 42) / 2, 42, 42); // 팀원 색 동그라미(지름 42)
                g.setColor(Color.WHITE);
                g.setFont(UiKit.font(Font.BOLD, 18));
                FontMetrics fm = g.getFontMetrics();
                String s = me.getName().substring(0, 1); // 이름 첫 글자
                // 글자 너비의 절반만큼 왼쪽에서 시작해, 글자가 동그라미 가운데(x = 22)에 오게 한다.
                g.drawString(s, 22 - fm.stringWidth(s) / 2, getHeight() / 2 + fm.getAscent() / 2 - 2);
                g.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(46, 46));
        JLabel name = UiKit.label(me.getName(), Font.BOLD, 17, UiKit.INK);
        JPanel text = UiKit.clear(new BorderLayout());
        text.add(name, BorderLayout.CENTER);
        text.add(profileSub, BorderLayout.SOUTH);
        UiKit.RoundButton logout = new UiKit.RoundButton("로그아웃", false);
        logout.setFont(UiKit.font(Font.BOLD, 11));
        logout.addActionListener(e -> {
            dispose();      // 이 창을 닫고
            onLogout.run(); // App이 넘겨준 동작(로그인 창 다시 띄우기)을 실행한다.
        });
        JPanel east = UiKit.clear(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        east.add(logout);
        card.add(avatar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(east, BorderLayout.EAST);
        return card;
    }

    /**
     * 모든 패널을 최신 내용으로 다시 그린다.
     *
     * <p>데이터가 바뀌는 동작 뒤에는 항상 이 메서드를 부른다. 각 패널은 바뀐 부분만 고치지 않고
     * {@link Ranch}에서 내용을 다시 읽어 그리므로, 화면과 데이터가 어긋나지 않는다.</p>
     */
    public final void refresh() {
        // 고른 할 일이 그사이 지워졌으면 선택을 푼다.
        Task sel = selectedId == null ? null : ranch.store().findTask(selectedId).orElse(null);
        if (sel == null) {
            selectedId = null;
        }
        // 프로필 한 줄: 내 양(제출한 할 일) 수와 기여도. git을 읽지 못한 상태면 기여도 대신 "–"를 보인다.
        long sheep = ranch.tasks().tasksOf(me.getId()).stream().filter(Task::isSubmitted).count();
        String pct = ranch.isGitReady() ? Math.round(ranch.contributionOf(me.getId()).getPercent()) + "%" : "–";
        profileSub.setText("양 " + sheep + "마리 · 기여도 " + pct);
        // 고른 할 일을 각 패널에 알린다. 패널은 선택을 기억하고 스스로 다시 그린다.
        taskList.setSelected(selectedId);
        ranchPanel.setSelected(selectedId);
        strip.setSelected(selectedId);
        detail.show(sel);
        grassPanel.repaint(); // 양털 화면은 선택과 상관없으므로 다시 그리기만 한다.
    }

    /** 패널에서 할 일(목록의 줄, 목장의 양, 팀원 줄의 양)이 눌렸을 때 불린다. 선택을 바꾸고 다시 그린다. */
    private void select(Task t) {
        selectedId = t.getId();
        refresh();
    }

    /**
     * 데이터를 바꾸는 모든 동작이 거치는 공통 길. 서비스를 부르고, 규칙에 걸리면 그 메시지를 보여 주고,
     * 성공하든 실패하든 화면을 다시 그린다. 규칙 검사(남의 할 일, 빈 제목 등)는 서비스가 하고, 화면은 메시지만 보여 준다.
     *
     * @param action      서비스 호출
     * @param selectAfter 성공했을 때 고를 할 일 번호(null이면 선택을 그대로 둔다)
     */
    private void act(Runnable action, String selectAfter) {
        try {
            action.run();
            if (selectAfter != null) {
                selectedId = selectAfter;
            }
        } catch (JandiException ex) { // 서비스는 사용자에게 알릴 문제를 이 예외로 던진다.
            JOptionPane.showMessageDialog(this, ex.getMessage(), "잔디", JOptionPane.INFORMATION_MESSAGE);
        }
        refresh();
    }

    /** 할 일 추가 대화상자를 띄우고, 확인을 누르면 추가한다. */
    private void addTask() {
        JTextField title = new JTextField(22);
        JTextArea memo = new JTextArea(4, 22);
        memo.setLineWrap(true);
        // JOptionPane의 메시지로 배열을 주면 문자열은 라벨로, 컴포넌트는 그대로, 위에서 아래로 놓인다.
        // 그래서 대화상자 클래스를 따로 만들지 않고도 간단한 입력 폼이 된다.
        Object[] form = {"할 일 제목", title, "메모 (한 일, 나중에 적어도 됩니다)", new JScrollPane(memo)};
        if (JOptionPane.showConfirmDialog(this, form, "할 일 추가", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            // 새 할 일의 번호는 만들어진 뒤에야 알 수 있어서, 람다 안에서 바로 선택 번호로 넣는다(selectAfter는 null).
            // 제목이 비었으면 TaskService가 예외를 던지고 act()가 그 메시지를 보여 준다.
            act(() -> selectedId = ranch.tasks().add(me.getId(), title.getText(), memo.getText()).getId(), null);
        }
    }

    /** 메모 고치기 대화상자를 띄우고, 확인을 누르면 저장한다. */
    private void editMemo(Task t) {
        JTextArea memo = new JTextArea(t.getMemo(), 5, 24);
        memo.setLineWrap(true);
        Object[] form = {"'" + t.getTitle() + "' 메모", new JScrollPane(memo)};
        if (JOptionPane.showConfirmDialog(this, form, "메모", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            act(() -> ranch.tasks().updateMemo(me.getId(), t.getId(), memo.getText()), t.getId());
        }
    }

    /** 상세 카드의 제출 버튼. 양이 박스 안으로 들어간다. t는 상세 카드가 보여 주던 할 일이다(없으면 null). */
    private void submit(Task t) {
        if (t != null) {
            act(() -> ranch.tasks().submit(me.getId(), t.getId()), t.getId());
        }
    }

    /** 상세 카드의 "양 내보내기" 버튼. 제출을 취소해 양이 박스 밖으로 나온다. */
    private void withdraw(Task t) {
        if (t != null) {
            act(() -> ranch.tasks().withdraw(me.getId(), t.getId()), t.getId());
        }
    }

    /** 상세 카드의 삭제 버튼. 한 번 더 물어본 뒤 지운다. 지운 할 일의 선택은 refresh()가 푼다. */
    private void delete(Task t) {
        if (t != null && JOptionPane.showConfirmDialog(this, "'" + t.getTitle() + "' 할 일을 지울까요?", "잔디",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            act(() -> ranch.tasks().delete(me.getId(), t.getId()), null);
        }
    }
}
