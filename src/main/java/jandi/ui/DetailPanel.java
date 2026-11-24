package jandi.ui;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 오른쪽 상세 카드. 선택 전에는 목장 안내를, 선택 후에는 할 일 정보와 메모를 보여 준다.
 * 제출·삭제·내보내기는 메인 창에 전달한다.
 */
public class DetailPanel extends UiKit.Card {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final Color PASTURE = UiKit.mix(UiKit.PEN, UiKit.CREAM, 0.38);
    private final Ranch ranch;
    private final Member me;
    private final JLabel title = UiKit.label("", Font.PLAIN, 23, UiKit.BROWN);
    private final JPanel guide = UiKit.clear(new GridLayout(2, 1, 0, 8));
    private final JPanel details = UiKit.clear(new BorderLayout(0, 8));
    private final JLabel owner = UiKit.label("", Font.BOLD, 16, UiKit.INK);
    private final JLabel status = new UiKit.StatBadge("할 일 상태");
    private final JLabel created = UiKit.label("", Font.PLAIN, 14, UiKit.INK);
    private final JLabel submitted = UiKit.label("", Font.PLAIN, 14, UiKit.INK);
    private final JTextArea memo = new JTextArea(3, 1);
    private final JPanel actions = UiKit.clear(new BorderLayout(8, 0));
    private final UiKit.RoundButton submit = new UiKit.RoundButton("양 들여보내기 (제출)", true);
    private final UiKit.RoundButton delete = new UiKit.RoundButton("삭제", false);
    private final UiKit.RoundButton withdraw = new UiKit.RoundButton("양 내보내기", true);
    private Task task;

    /**
     * @param ranch 목장
     * @param me 로그인한 팀원
     * @param onSubmit 제출할 때 부를 동작
     * @param onDelete 지울 때 부를 동작
     * @param onWithdraw 내보낼 때 부를 동작
     */
    public DetailPanel(Ranch ranch, Member me, Consumer<Task> onSubmit, Consumer<Task> onDelete,
                       Consumer<Task> onWithdraw) {
        this.ranch = ranch;
        this.me = me;
        setLayout(new BorderLayout(0, 10));
        title.putClientProperty("html.disable", true);
        add(title, BorderLayout.NORTH);

        guide.add(guideBox(PASTURE, GuideIcon.SHEEP, "할 일 보기",
                "목장이나 아래 팀원 줄에서<br>양을 누르면 할 일이 보여요."));
        guide.add(guideBox(UiKit.SOFT, GuideIcon.FENCE, "울타리 크기",
                "팀원의 Git 기여도가 높을수록<br>울타리가 넓어져요."));

        JPanel info = box(PASTURE, new BorderLayout(0, 5));
        JPanel who = UiKit.clear(new BorderLayout(6, 0));
        who.add(owner, BorderLayout.CENTER);
        who.add(status, BorderLayout.EAST);
        info.add(who, BorderLayout.NORTH);
        JPanel dates = UiKit.clear(null);
        dates.setLayout(new BoxLayout(dates, BoxLayout.Y_AXIS));
        dates.add(created);
        dates.add(submitted);
        info.add(dates, BorderLayout.CENTER);
        details.add(info, BorderLayout.NORTH);

        JPanel note = box(UiKit.SOFT, new BorderLayout(0, 4));
        JLabel noteTitle = UiKit.label("한 일", Font.BOLD, 17, UiKit.BROWN);
        noteTitle.setLabelFor(memo);
        note.add(noteTitle, BorderLayout.NORTH);
        memo.setEditable(false);
        memo.setLineWrap(true);
        memo.setWrapStyleWord(true);
        memo.setFont(UiKit.font(Font.PLAIN, 15));
        memo.setOpaque(false);
        memo.setBorder(null);
        JScrollPane scroll = new JScrollPane(memo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        note.add(scroll, BorderLayout.CENTER);
        details.add(note, BorderLayout.CENTER);

        JPanel body = UiKit.clear(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(guide);
        body.add(details);
        add(body, BorderLayout.CENTER);

        submit.addActionListener(e -> onSubmit.accept(task));
        delete.addActionListener(e -> onDelete.accept(task));
        withdraw.addActionListener(e -> onWithdraw.accept(task));
        actions.add(submit, BorderLayout.CENTER);
        actions.add(delete, BorderLayout.EAST);
        actions.add(withdraw, BorderLayout.NORTH);
        add(actions, BorderLayout.SOUTH);
        show(null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(310, super.getPreferredSize().height);
    }

    private static JPanel box(Color fill, LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = UiKit.aa(g0.create());
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g.setColor(UiKit.mix(fill, UiKit.BROWN, 0.21));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 12));
        return panel;
    }

    private static JPanel guideBox(Color fill, GuideIcon icon, String heading, String description) {
        JPanel panel = box(fill, new BorderLayout(10, 0));
        panel.add(new JLabel(icon), BorderLayout.WEST);
        JPanel copy = UiKit.clear(new BorderLayout(0, 3));
        copy.add(UiKit.label(heading, Font.BOLD, 17, UiKit.BROWN), BorderLayout.NORTH);
        copy.add(UiKit.label("<html>" + description + "</html>", Font.PLAIN, 15, UiKit.INK), BorderLayout.CENTER);
        panel.add(copy, BorderLayout.CENTER);
        return panel;
    }

    /** 선택한 할 일을 표시한다. 같은 할 일을 새로 고칠 때는 메모를 읽던 위치를 유지한다. */
    public final void show(Task t) {
        boolean changed = task == null || t == null || !task.getId().equals(t.getId());
        task = t;
        guide.setVisible(t == null);
        details.setVisible(t != null);
        title.setText(t == null ? "목장 안내" : t.getTitle());
        title.setToolTipText(t == null ? null : t.getTitle());
        boolean mine = t != null && t.getOwnerId().equals(me.getId());
        actions.setVisible(mine);
        submit.setVisible(mine && !t.isSubmitted());
        submit.setEnabled(submit.isVisible());
        delete.setVisible(submit.isVisible());
        withdraw.setVisible(mine && t.isSubmitted());
        if (t != null) {
            String name = ranch.store().findMember(t.getOwnerId()).map(Member::getName).orElse("?");
            owner.setText("담당  " + name + (mine ? " (나)" : ""));
            status.setText(t.getStatus().label());
            created.setText("만든 날   " + WHEN.format(t.getCreatedAt()));
            submitted.setVisible(t.getSubmittedAt() != null);
            if (t.getSubmittedAt() != null) {
                submitted.setText("제출일   " + WHEN.format(t.getSubmittedAt()));
            }
            String text = t.getMemo().isBlank() ? "아직 적어 둔 메모가 없어요." : t.getMemo();
            memo.setForeground(t.getMemo().isBlank() ? UiKit.MUTED : UiKit.INK);
            if (changed || !text.equals(memo.getText())) {
                memo.setText(text);
                memo.setCaretPosition(0);
            }
        }
        revalidate();
        repaint();
    }

    private enum GuideIcon implements Icon {
        SHEEP, FENCE;

        @Override public int getIconWidth() { return 48; }
        @Override public int getIconHeight() { return 48; }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = UiKit.aa(graphics.create());
            g.translate(x, y);
            g.setColor(UiKit.mix(UiKit.PEN, UiKit.GRASS_DARK, 0.28));
            g.fillOval(2, 28, 44, 15);
            if (this == SHEEP) {
                Sprites.sheep(g, 24, 23, 24, UiKit.CREAM, false, Sprites.IDLE, false);
            } else {
                g.setColor(UiKit.BROWN);
                g.fillRoundRect(4, 17, 40, 6, 3, 3);
                g.fillRoundRect(4, 27, 40, 6, 3, 3);
                for (int post : new int[]{7, 21, 35}) {
                    g.setColor(UiKit.BROWN);
                    g.fillRoundRect(post, 11, 7, 25, 3, 3);
                    g.setColor(new Color(0xD6A16B));
                    g.fillRoundRect(post + 1, 12, 5, 22, 2, 2);
                }
            }
            g.dispose();
        }
    }
}
