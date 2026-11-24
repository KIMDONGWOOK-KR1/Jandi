package jandi.ui;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.domain.TaskStatus;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
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
 * <p>이 패널은 데이터를 직접 바꾸지 않는다. 무엇이 눌렸는지만 {@link Actions}와 {@code onSelect}로 알리고,
 * 실제 처리는 그것을 넘겨준 쪽({@link MainFrame})이 한다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public class TaskListPanel extends UiKit.Card {

    /** 할 일 목록에서 일어나는 동작을 받는 쪽. 메인 창이 구현해서 생성자에 넘긴다. */
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
    /** 할 일 줄(Row)을 위에서 아래로 쌓는 패널. */
    private final JPanel list = new JPanel();
    /** 고른 할 일 번호(없으면 null). 그 줄에 테두리를 그린다. */
    private String selectedId;

    /**
     * 머리줄(제목과 추가 버튼)과 스크롤되는 목록을 배치한다.
     *
     * @param ranch    목장
     * @param me       로그인한 팀원
     * @param actions  추가·완료 체크·메모를 눌렀을 때 부를 동작
     * @param onSelect 줄을 눌렀을 때 부를 동작
     */
    public TaskListPanel(Ranch ranch, Member me, Actions actions, Consumer<Task> onSelect) {
        this.ranch = ranch;
        this.me = me;
        this.actions = actions;
        this.onSelect = onSelect;
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 18, 16));
        getAccessibleContext().setAccessibleName("내 할 일");
        UiKit.RoundButton add = new UiKit.RoundButton("+", true);
        add.setFont(UiKit.font(Font.PLAIN, 23));
        add.setToolTipText("할 일 추가");
        add.getAccessibleContext().setAccessibleName("할 일 추가");
        add.setPreferredSize(new Dimension(40, 35));
        add.addActionListener(e -> actions.add()); // 눌렸다는 것만 알린다. 입력 창은 메인 창이 띄운다.
        JPanel head = UiKit.clear(new BorderLayout());
        head.add(add, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);
        // 목록: BoxLayout(Y_AXIS)으로 줄을 세로로 쌓는다.
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        // 목록을 NORTH에 넣어, 줄이 적을 때도 위쪽에 모여 있게 한다.
        JPanel holder = UiKit.clear(new BorderLayout());
        holder.add(list, BorderLayout.NORTH);
        // 줄이 많아지면 세로로 스크롤된다. 카드 바탕이 비치도록 스크롤 영역은 투명하게 한다.
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

    /** 목록을 다시 그린다. 줄을 모두 지우고 {@link Ranch}에서 내 할 일을 다시 읽어 새로 만든다. */
    public final void reload() {
        list.removeAll();
        for (Task t : ranch.tasks().tasksOf(me.getId())) {
            list.add(new Row(t));
            list.add(Box.createVerticalStrut(6));
        }
        if (list.getComponentCount() == 0) { // 할 일이 하나도 없으면 안내 문구를 보인다.
            JLabel empty = new JLabel("<html>아직 할 일이 없습니다.<br>+ 를 눌러 추가해 보세요.</html>");
            empty.setForeground(UiKit.MUTED);
            list.add(empty);
        }
        list.revalidate(); // 컴포넌트가 바뀌었으니 배치를 다시 계산하고
        list.repaint();    // 화면을 다시 그린다.
    }

    /**
     * 할 일 한 줄: 체크 동그라미 + 제목 + 오른쪽 글자("메모" 또는 "제출됨").
     * 안쪽 클래스라서 바깥 클래스의 actions, onSelect, selectedId를 그대로 쓴다.
     */
    private final class Row extends JPanel {
        private final boolean selected;
        private boolean hovered;

        Row(Task t) {
            super(new BorderLayout(14, 0));
            selected = t.getId().equals(selectedId);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 9));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); // BoxLayout 안에서 높이는 44로 묶고 너비는 끝까지 늘린다.
            setPreferredSize(new Dimension(100, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Check check = new Check(t);
            JLabel name = UiKit.label(t.getTitle(), Font.PLAIN, 24, UiKit.BROWN);
            name.setToolTipText(t.getTitle());
            JComponent side;
            if (t.isSubmitted()) {
                side = UiKit.label("제출됨", Font.PLAIN, 12, UiKit.MUTED);
            } else {
                JButton memo = new JButton("메모") {
                    @Override
                    protected void paintComponent(Graphics g) {
                        if (hovered || selected || isFocusOwner()) {
                            super.paintComponent(g);
                        }
                    }
                };
                memo.setFont(UiKit.font(Font.PLAIN, 12));
                memo.setForeground(UiKit.BROWN);
                memo.setContentAreaFilled(false);
                memo.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
                memo.setToolTipText("메모 수정");
                memo.getAccessibleContext().setAccessibleName(t.getTitle() + " 메모 수정");
                memo.addActionListener(e -> actions.memo(t));
                side = memo;
            }
            add(check, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            add(side, BorderLayout.EAST);
            MouseAdapter click = new MouseAdapter() { // 눌린 곳(이벤트 소스)에 따라 동작을 나눈다.
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getSource() == check) {
                        actions.toggle(t); // 체크 동그라미 → 완료 켜기/끄기
                    } else if (!(e.getSource() instanceof JButton)) {
                        onSelect.accept(t); // 그 밖의 곳 → 이 할 일을 고른다
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), Row.this));
                    repaint();
                }
            };
            // 줄과 그 안의 컴포넌트가 리스너 하나를 함께 쓴다.
            addMouseListener(click);
            name.addMouseListener(click);
            side.addMouseListener(click);
            check.addMouseListener(click);
        }

        /** 옅은 둥근 바탕을 그리고, 고른 줄이면 갈색 테두리를 두른다. 체크와 글자는 그 위에 Swing이 그린다. */
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UiKit.aa(g0.create());
            g.setColor(UiKit.SOFT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (selected) {
                g.setColor(UiKit.BROWN);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            }
            g.dispose();
        }
    }

    /** 완료 체크 동그라미. 체크된 상태면 초록 바탕에 흰 체크를, 아니면 빈 동그라미를 직접 그린다. */
    private static final class Check extends JComponent {
        private final boolean on;

        Check(Task t) {
            on = t.getStatus() != TaskStatus.TODO; // 완료(DONE)와 제출됨(SUBMITTED) 모두 체크된 모양이다.
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
                g.drawLine(6, y + 10, 9, y + 14);  // 체크 표시의 짧은 획
                g.drawLine(9, y + 14, 15, y + 6);  // 체크 표시의 긴 획
            } else {
                g.setColor(UiKit.MUTED);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(2, y + 1, 18, 18);
            }
            g.dispose();
        }
    }
}
