package jandi.ui;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.Ranch;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 아래쪽 팀원 줄. 팀원마다 제출 현황과 기여도, 양들을 한 줄로 보여 준다. 양을 누르면 그 할 일을 고른다.
 *
 * @author Team 3
 * @version 0.1
 */
public class TeamStrip extends UiKit.Card {

    private static final long serialVersionUID = 1L;
    private final transient Ranch ranch;
    private final transient Map<Rectangle, Task> hits = new LinkedHashMap<>();
    private String selectedId;

    /**
     * @param ranch    목장
     * @param onSelect 양을 눌렀을 때 부를 동작
     */
    public TeamStrip(Ranch ranch, Consumer<Task> onSelect) {
        this.ranch = ranch;
        setPreferredSize(new Dimension(100, 150));
        UiKit.listenTaskClicks(this, hits, onSelect);
    }

    /** @param taskId 고른 할 일 번호(없으면 null) */
    public void setSelected(String taskId) {
        this.selectedId = taskId;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = UiKit.aa(g0.create());
        hits.clear();
        List<Member> ms = ranch.members();
        int colW = (getWidth() - 24) / Math.max(1, ms.size());
        for (int i = 0; i < ms.size(); i++) {
            Member m = ms.get(i);
            int x = 12 + i * colW;
            if (i > 0) {
                g.setColor(UiKit.mix(UiKit.BROWN, UiKit.CREAM, 0.5));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[]{2f, 5f}, 0f));
                g.drawLine(x, 22, x, getHeight() - 26);
            }
            List<Task> tasks = ranch.tasks().tasksOf(m.getId());
            long sub = tasks.stream().filter(Task::isSubmitted).count();
            g.setFont(UiKit.font(Font.BOLD, 15));
            g.setColor(UiKit.INK);
            g.drawString(m.getName(), x + 18, 34);
            int nx = x + 18 + g.getFontMetrics().stringWidth(m.getName()) + 8;
            g.setFont(UiKit.font(Font.PLAIN, 11));
            g.setColor(UiKit.MUTED);
            String pct = ranch.isGitReady() ? Math.round(ranch.contributionOf(m.getId()).getPercent()) + "%" : "–";
            g.drawString("제출 " + sub + "/" + tasks.size() + " · 기여도 " + pct, nx, 33);
            Color base = new Color(m.getColor());
            Sprites.farmer(g, x + 40, 118, 62, base);
            int step = 62;
            int fitN = Math.max(1, (colW - 86) / step);
            int show = tasks.size() > fitN ? fitN - 1 : tasks.size();
            Color wool = UiKit.mix(base, Color.WHITE, 0.62);
            for (int k = 0; k < show; k++) {
                Task t = tasks.get(k);
                int cx = x + 104 + k * step;
                int cy = 78;
                if (t.getId().equals(selectedId)) {
                    g.setColor(UiKit.SOFT);
                    g.fillRoundRect(cx - 29, cy - 24, 58, 66, 12, 12);
                }
                Sprites.sheep(g, cx, cy, 32, t.isSubmitted() ? wool : new Color(0xFFFDF6), !t.isSubmitted());
                g.setFont(UiKit.font(Font.PLAIN, 10));
                FontMetrics fm = g.getFontMetrics();
                String s = UiKit.fit(fm, t.getTitle(), step - 6);
                g.setColor(t.isSubmitted() ? UiKit.INK : UiKit.MUTED);
                g.drawString(s, cx - fm.stringWidth(s) / 2, cy + 34);
                hits.put(new Rectangle(cx - 29, cy - 24, 58, 66), t);
            }
            if (show < tasks.size()) {
                g.setFont(UiKit.font(Font.BOLD, 12));
                g.setColor(UiKit.MUTED);
                g.drawString("+" + (tasks.size() - show), x + 104 + show * step - 12, 84);
            }
        }
        g.dispose();
    }
}
