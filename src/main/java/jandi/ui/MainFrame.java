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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 메인 창. 왼쪽에 내 정보와 할 일, 가운데에 목장/양털, 오른쪽에 상세, 아래에 팀원 줄이 있다.
 *
 * @author Team 3
 * @version 0.1
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final transient Ranch ranch;
    private final transient Member me;
    private final RanchPanel ranchPanel;
    private final GrassPanel grassPanel;
    private final TeamStrip strip;
    private final TaskListPanel taskList;
    private final DetailPanel detail;
    private final JLabel profileSub = new JLabel();
    private String selectedId;

    /**
     * @param ranch    목장
     * @param me       로그인한 팀원
     * @param onLogout 로그아웃했을 때 부를 동작
     */
    public MainFrame(Ranch ranch, Member me, Runnable onLogout) {
        super("잔디 — 팀 기여도 목장");
        this.ranch = ranch;
        this.me = me;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(18, 12)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                UiKit.paintGrass(UiKit.aa(g), getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        ranchPanel = new RanchPanel(ranch, me, this::select);
        grassPanel = new GrassPanel(ranch);
        strip = new TeamStrip(ranch, this::select);
        detail = new DetailPanel(ranch, me, this::submit, this::delete, this::withdraw);
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
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(290, 100));
        left.add(profileCard(onLogout), BorderLayout.NORTH);
        left.add(taskList, BorderLayout.CENTER);

        // 가운데: 탭 + 목장/양털
        CardLayout cards = new CardLayout();
        JPanel center = new JPanel(cards);
        center.setOpaque(false);
        center.add(ranchPanel, "ranch");
        center.add(grassPanel, "grass");
        UiKit.RoundButton tabRanch = new UiKit.RoundButton("목장", true);
        UiKit.RoundButton tabGrass = new UiKit.RoundButton("양털", false);
        UiKit.RoundButton repo = new UiKit.RoundButton("git 저장소", false);
        tabRanch.addActionListener(e -> {
            cards.show(center, "ranch");
            tabRanch.setPrimary(true);
            tabGrass.setPrimary(false);
        });
        tabGrass.addActionListener(e -> {
            cards.show(center, "grass");
            tabRanch.setPrimary(false);
            tabGrass.setPrimary(true);
        });
        repo.addActionListener(e -> {
            new RepoDialog(this, ranch).setVisible(true);
            refresh();
        });
        UiKit.Card tabs = new UiKit.Card();
        tabs.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        tabs.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));
        tabs.add(tabRanch);
        tabs.add(tabGrass);
        tabs.add(repo);
        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabRow.setOpaque(false);
        tabRow.add(tabs);
        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setOpaque(false);
        mid.add(tabRow, BorderLayout.NORTH);
        mid.add(center, BorderLayout.CENTER);

        // 오른쪽: 상세
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(detail, BorderLayout.NORTH);

        root.add(left, BorderLayout.WEST);
        root.add(mid, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        root.add(strip, BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1400, 880);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                ranch.refreshGit(); // 창으로 돌아오면 새 커밋을 다시 읽는다.
                refresh();
            }
        });
        refresh();
    }

    private UiKit.Card profileCard(Runnable onLogout) {
        UiKit.Card card = new UiKit.Card();
        card.setLayout(new BorderLayout(10, 0));
        Color mine = new Color(me.getColor());
        JPanel avatar = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = UiKit.aa(g0.create());
                g.setColor(mine);
                g.fillOval(1, (getHeight() - 42) / 2, 42, 42);
                g.setColor(Color.WHITE);
                g.setFont(UiKit.font(Font.BOLD, 18));
                FontMetrics fm = g.getFontMetrics();
                String s = me.getName().substring(0, 1);
                g.drawString(s, 22 - fm.stringWidth(s) / 2, getHeight() / 2 + fm.getAscent() / 2 - 2);
                g.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(46, 46));
        JLabel name = new JLabel(me.getName());
        name.setFont(UiKit.font(Font.BOLD, 17));
        name.setForeground(UiKit.INK);
        profileSub.setFont(UiKit.font(Font.PLAIN, 11));
        profileSub.setForeground(UiKit.MUTED);
        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        text.add(name, BorderLayout.CENTER);
        text.add(profileSub, BorderLayout.SOUTH);
        UiKit.RoundButton logout = new UiKit.RoundButton("로그아웃", false);
        logout.setFont(UiKit.font(Font.BOLD, 11));
        logout.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        east.setOpaque(false);
        east.add(logout);
        card.add(avatar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(east, BorderLayout.EAST);
        return card;
    }

    /** 모든 패널을 최신 내용으로 다시 그린다. */
    public final void refresh() {
        Task sel = selectedId == null ? null : ranch.store().findTask(selectedId).orElse(null);
        if (sel == null) {
            selectedId = null;
        }
        long sheep = ranch.tasks().tasksOf(me.getId()).stream().filter(Task::isSubmitted).count();
        String pct = ranch.isGitReady() ? Math.round(ranch.contributionOf(me.getId()).getPercent()) + "%" : "–";
        profileSub.setText("양 " + sheep + "마리 · 기여도 " + pct);
        taskList.setSelected(selectedId);
        ranchPanel.setSelected(selectedId);
        strip.setSelected(selectedId);
        detail.show(sel);
        grassPanel.repaint();
    }

    private void select(Task t) {
        selectedId = t.getId();
        refresh();
    }

    private void act(Runnable action, String selectAfter) {
        try {
            action.run();
            if (selectAfter != null) {
                selectedId = selectAfter;
            }
        } catch (JandiException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "잔디", JOptionPane.INFORMATION_MESSAGE);
        }
        refresh();
    }

    private void addTask() {
        JTextField title = new JTextField(22);
        JTextArea memo = new JTextArea(4, 22);
        memo.setLineWrap(true);
        Object[] form = {"할 일 제목", title, "메모 (한 일, 나중에 적어도 됩니다)", new JScrollPane(memo)};
        if (JOptionPane.showConfirmDialog(this, form, "할 일 추가", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                selectedId = ranch.tasks().add(me.getId(), title.getText(), memo.getText()).getId();
            } catch (JandiException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "잔디", JOptionPane.INFORMATION_MESSAGE);
            }
            refresh();
        }
    }

    private void editMemo(Task t) {
        JTextArea memo = new JTextArea(t.getMemo(), 5, 24);
        memo.setLineWrap(true);
        Object[] form = {"'" + t.getTitle() + "' 메모", new JScrollPane(memo)};
        if (JOptionPane.showConfirmDialog(this, form, "메모", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            act(() -> ranch.tasks().updateMemo(me.getId(), t.getId(), memo.getText()), t.getId());
        }
    }

    private void submit(Task t) {
        if (t != null) {
            act(() -> ranch.tasks().submit(me.getId(), t.getId()), t.getId());
        }
    }

    private void withdraw(Task t) {
        if (t != null) {
            act(() -> ranch.tasks().withdraw(me.getId(), t.getId()), t.getId());
        }
    }

    private void delete(Task t) {
        if (t != null && JOptionPane.showConfirmDialog(this, "'" + t.getTitle() + "' 할 일을 지울까요?", "잔디",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            act(() -> ranch.tasks().delete(me.getId(), t.getId()), null);
        }
    }
}
