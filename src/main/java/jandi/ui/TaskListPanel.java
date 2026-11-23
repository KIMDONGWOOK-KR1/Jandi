package jandi.ui;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.domain.TaskStatus;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * 왼쪽 "내 할 일" 카드. 할 일을 추가하고, 완료 체크하고, 메모를 고친다.
 *
 * @author Team 3
 * @version 0.1
 */
public class TaskListPanel extends UiKit.Card {

    /** 할 일 목록에서 일어나는 동작을 받는 쪽. */
    public interface Actions {
        /** 새 할 일 추가를 눌렀다. */
        void add();

        /**
         * 완료 체크를 눌렀다.
         *
         * @param t 할 일
         */
        void toggle(Task t);

        /**
         * 메모를 눌렀다.
         *
         * @param t 할 일
         */
        void memo(Task t);
    }

    private final Ranch ranch;
    private final Member me;
    private final Actions actions;
    private final Consumer<Task> onSelect;
    private final JPanel list = new JPanel();
    private String selectedId;

    /**
     * @param ranch    목장
     * @param me       로그인한 팀원
     * @param actions  버튼 동작
     * @param onSelect 줄을 눌렀을 때 부를 동작
     */
    public TaskListPanel(Ranch ranch, Member me, Actions actions, Consumer<Task> onSelect) {
        this.ranch = ranch;
        this.me = me;
        this.actions = actions;
        this.onSelect = onSelect;
        setLayout(new BorderLayout(0, 10));
        JLabel title = UiKit.label("내 할 일", Font.BOLD, 18, UiKit.INK);
        UiKit.RoundButton add = new UiKit.RoundButton("+", true);
        add.setFont(UiKit.font(Font.BOLD, 18));
        add.setToolTipText("할 일 추가");
        add.setPreferredSize(new Dimension(40, 36));
        add.addActionListener(e -> actions.add());
        JPanel head = UiKit.clear(new BorderLayout());
        head.add(title, BorderLayout.WEST);
        head.add(add, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JPanel holder = UiKit.clear(new BorderLayout());
        holder.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);
        reload();
    }

    /** @param taskId 고른 할 일 번호(없으면 null) */
    public void setSelected(String taskId) {
        this.selectedId = taskId;
        reload();
    }

    /** 목록을 다시 그린다. */
    public final void reload() {
        list.removeAll();
        for (Task t : ranch.tasks().tasksOf(me.getId())) {
            list.add(new Row(t));
            list.add(Box.createVerticalStrut(8));
        }
        if (list.getComponentCount() == 0) {
            JLabel empty = new JLabel("<html>아직 할 일이 없습니다.<br>+ 를 눌러 추가해 보세요.</html>");
            empty.setForeground(UiKit.MUTED);
            list.add(empty);
        }
        list.revalidate();
        list.repaint();
    }

    private final class Row extends JPanel {
        private final boolean selected;

        Row(Task t) {
            super(new BorderLayout(8, 0));
            selected = t.getId().equals(selectedId);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setPreferredSize(new Dimension(100, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Check check = new Check(t);
            JLabel name = UiKit.label(t.getTitle(), Font.BOLD, 14, UiKit.INK);
            JLabel side = UiKit.label(t.isSubmitted() ? "제출됨" : "메모", Font.PLAIN, 11,
                    t.isSubmitted() ? UiKit.MUTED : UiKit.BROWN);
            add(check, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            add(side, BorderLayout.EAST);
            MouseAdapter click = new MouseAdapter() { // 눌린 곳(이벤트 소스)에 따라 동작을 나눈다.
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getSource() == check) {
                        actions.toggle(t);
                    } else if (e.getSource() == side && !t.isSubmitted()) {
                        actions.memo(t);
                    } else {
                        onSelect.accept(t);
                    }
                }
            };
            addMouseListener(click);
            name.addMouseListener(click);
            side.addMouseListener(click);
            check.addMouseListener(click);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UiKit.aa(g0.create());
            g.setColor(UiKit.SOFT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            if (selected) {
                g.setColor(UiKit.BROWN);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            }
            g.dispose();
        }
    }

    private static final class Check extends JComponent {
        private final boolean on;

        Check(Task t) {
            on = t.getStatus() != TaskStatus.TODO;
            setPreferredSize(new Dimension(22, 22));
            setToolTipText(t.isSubmitted() ? "제출됨" : on ? "완료 취소" : "완료하기");
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UiKit.aa(g0.create());
            int y = (getHeight() - 20) / 2;
            g.setColor(on ? UiKit.GREEN : Color.WHITE);
            g.fillOval(1, y, 20, 20);
            if (on) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(6, y + 10, 9, y + 14);
                g.drawLine(9, y + 14, 15, y + 6);
            } else {
                g.setColor(UiKit.MUTED);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(2, y + 1, 18, 18);
            }
            g.dispose();
        }
    }
}
