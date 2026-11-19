package jandi.ui;

import jandi.domain.Contribution;
import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.Ranch;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 목장 화면. 팀원마다 박스가 하나씩 있고, 박스 크기는 git 기여도에 비례한다.
 *
 * <p>제출한 할 일은 양이 되어 담당자의 박스 안에 들어가고, 아직 제출하지 않은 양은 박스 아래에 흐리게 서 있다.
 * 양을 누르면 그 할 일을 고른다. 다른 팀원의 양도 누를 수 있다.</p>
 *
 * <p>농부는 자기 박스의 가운데 쪽 가장자리 옆에서 조금씩 걸어 다니고, 양들도 제자리 근처에서 걷고 풀을 뜯는다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public class RanchPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final transient Ranch ranch;
    private final transient Member me;
    private final transient Map<Rectangle, Task> hits = new LinkedHashMap<>();
    private final transient Map<String, Walker> walkers = new HashMap<>();
    private final transient Map<String, Rectangle> pens = new HashMap<>();
    private final transient Random random = new Random();
    private final Timer timer = new Timer(70, e -> tick());
    private int tick;
    private String selectedId;

    /** 걸어 다니는 농부 한 명의 상태. 위치는 자기 박스 옆 기준 자리에서 벗어난 거리(픽셀)이다. */
    private static final class Walker {
        private double x;
        private double y;
        private double tx;
        private double ty;
        private int pause;
        private boolean left;
        private boolean watering;
        private boolean moving;
    }

    /** 농부가 기준 자리에서 벗어날 수 있는 가로 거리(픽셀). */
    public static final int ROAM_X = 26;
    /** 농부가 기준 자리에서 벗어날 수 있는 세로 거리(픽셀). */
    public static final int ROAM_Y = 20;

    /**
     * 농부의 기준 자리. 자기 박스의 가운데 쪽 모서리 근처라, 박스에서 멀어지지 않으면서 화면 가운데 쪽에 선다.
     *
     * @param index 팀원 순번
     * @param count 팀원 수
     * @param pen   그 팀원의 박스
     * @return 발끝 위치 {x, y}
     */
    public static int[] home(int index, int count, Rectangle pen) {
        int rows = Math.max(1, (count + 1) / 2);
        boolean leftColumn = index % 2 == 0;
        boolean upper = index / 2 < rows / 2.0;
        if (upper) {
            // 위쪽 줄: 박스 아래 변의 가운데 쪽 모서리에 선다. 두 박스 사이가 좁아도 서로 겹치지 않는다.
            return new int[]{leftColumn ? pen.x + pen.width - 34 : pen.x + 34, pen.y + pen.height + 16};
        }
        // 아래쪽 줄: 왼쪽 열은 박스 위 변의 가운데 쪽 모서리, 오른쪽 열은 왼쪽 위 이름표를 피해 박스 왼쪽 아래에 선다.
        int x = leftColumn ? pen.x + pen.width - 30 : pen.x - 30;
        int y = leftColumn ? pen.y + 4 : pen.y + (int) (pen.height * 0.9);
        return new int[]{x, y};
    }

    private void tick() {
        tick++;
        for (Member m : ranch.members()) {
            Walker w = walkers.computeIfAbsent(m.getId(), k -> {
                Walker n = new Walker();
                n.pause = 10 + random.nextInt(50);
                return n;
            });
            if (w.pause > 0) {
                w.moving = false;
                if (--w.pause == 0) {
                    w.watering = false;
                    w.tx = (random.nextDouble() * 2 - 1) * ROAM_X;
                    w.ty = (random.nextDouble() * 2 - 1) * ROAM_Y;
                }
                continue;
            }
            double dx = w.tx - w.x;
            double dy = w.ty - w.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 1.5) {
                w.pause = 30 + random.nextInt(70);
                w.watering = random.nextInt(4) == 0;
                w.moving = false;
            } else {
                w.moving = true;
                w.left = dx < 0;
                w.x += dx / dist * 1.1;
                w.y += dy / dist * 1.1;
            }
        }
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /**
     * @param ranch    목장
     * @param me       로그인한 팀원
     * @param onSelect 양을 눌렀을 때 부를 동작
     */
    public RanchPanel(Ranch ranch, Member me, Consumer<Task> onSelect) {
        this.ranch = ranch;
        this.me = me;
        setOpaque(false);
        UiKit.listenTaskClicks(this, hits, onSelect);
    }

    /** @param taskId 고른 할 일 번호(없으면 null) */
    public void setSelected(String taskId) {
        this.selectedId = taskId;
        repaint();
    }

    /**
     * 기여도로 박스 크기 비율을 구한다. 가장 많이 기여한 팀원이 1이고, 넓이가 기여도에 비례한다.
     *
     * @param percent 이 팀원의 기여도
     * @param max     팀에서 가장 높은 기여도
     * @return 한 변의 비율(0.42~1)
     */
    public static double penScale(double percent, double max) {
        if (max <= 0) {
            return 0.7;
        }
        return Math.max(0.42, Math.min(1, Math.sqrt(percent / max)));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UiKit.aa(g0.create());
        hits.clear();
        List<Member> ms = ranch.members();
        int rows = (ms.size() + 1) / 2;
        int cw = getWidth() / 2;
        int ch = getHeight() / Math.max(1, rows);
        double max = ranch.contributions().values().stream().mapToDouble(Contribution::getPercent).max().orElse(0);
        boolean git = ranch.isGitReady() && max > 0;
        for (int i = 0; i < ms.size(); i++) {
            Member m = ms.get(i);
            Contribution c = ranch.contributionOf(m.getId());
            double s = git ? penScale(c.getPercent(), max) : 0.7;
            int maxW = (int) (cw * 0.9);
            int maxH = Math.max(90, ch - 34 - 96);
            int pw = (int) (maxW * s);
            int ph = (int) (maxH * s);
            int px = (i % 2) * cw + (cw - pw) / 2;
            int py = (i / 2) * ch + 30 + (maxH - ph) / 2;
            pens.put(m.getId(), new Rectangle(px, py, pw, ph));
            paintPen(g, m, c, git, px, py, pw, ph);
            List<Task> all = ranch.tasks().tasksOf(m.getId());
            List<Task> in = all.stream().filter(Task::isSubmitted).collect(Collectors.toList());
            List<Task> out = all.stream().filter(t -> !t.isSubmitted()).collect(Collectors.toList());
            paintInside(g, m, in, px, py, pw, ph);
            paintOutside(g, out, (i % 2) * cw, (i / 2) * ch + ch - 46, cw);
        }
        paintFarmers(g, ms);
        g.dispose();
    }

    private void paintFarmers(Graphics2D g, List<Member> ms) {
        List<Integer> order = new ArrayList<>();
        Map<Integer, int[]> pos = new HashMap<>();
        for (int i = 0; i < ms.size(); i++) {
            Walker w = walkers.get(ms.get(i).getId());
            int[] at = home(i, ms.size(), pens.get(ms.get(i).getId()));
            if (w != null) {
                at[0] += (int) Math.round(w.x);
                at[1] += (int) Math.round(w.y);
            }
            pos.put(i, at);
            order.add(i);
        }
        order.sort(Comparator.comparingInt(i -> pos.get(i)[1])); // 아래에 있는 농부를 나중에(앞에) 그린다.
        for (int i : order) {
            Member m = ms.get(i);
            Walker w = walkers.get(m.getId());
            int cx = pos.get(i)[0];
            int foot = pos.get(i)[1];
            int frame = Sprites.IDLE + ((tick / 4 + i * 11) % 36 == 0 ? 2 : 0); // 가끔 눈을 깜빡인다.
            if (w != null && w.moving) {
                frame = Sprites.WALK + (tick / 3) % Sprites.COLS;
            } else if (w != null && w.watering) {
                frame = Sprites.WORK + (tick / 5) % Sprites.COLS;
            }
            Color c = new Color(m.getColor());
            Sprites.farmer(g, cx, foot, 72, c, frame, w != null && w.left);
            g.setFont(UiKit.font(Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(m.getName()) + 12;
            g.setColor(Color.WHITE);
            g.fillRoundRect(cx - tw / 2, foot + 5, tw, 18, 8, 8);
            g.setColor(c);
            g.setStroke(new BasicStroke(1.6f));
            g.drawRoundRect(cx - tw / 2, foot + 5, tw, 18, 8, 8);
            g.setColor(UiKit.INK);
            g.drawString(m.getName(), cx - tw / 2 + 6, foot + 18);
        }
    }

    private void paintPen(Graphics2D g, Member m, Contribution c, boolean git, int x, int y, int w, int h) {
        boolean mine = m.getId().equals(me.getId());
        g.setColor(UiKit.PEN);
        g.fillRoundRect(x, y, w, h, 18, 18);
        Color own = new Color(m.getColor());
        paintFence(g, x, y, w, h, mine ? own : null);
        String label = m.getName() + (mine ? " (나)" : "") + " · "
                + (git ? Math.round(c.getPercent()) + "%" : "–");
        g.setFont(UiKit.font(Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label) + 16;
        g.setColor(mine ? UiKit.mix(own, UiKit.INK, 0.18) : UiKit.BROWN);
        g.fillRoundRect(x + 10, y - 13, tw, 24, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString(label, x + 18, y - 13 + 17);
    }

    /** 울타리 안 양의 크기. 양이 늘어도 바뀌지 않는다. */
    static final int SHEEP_SIZE = 44;

    private static final Color WOOD =new Color(0xD29A5E);
    private static final Color WOOD_LIGHT = new Color(0xE8BD85);
    private static final Color WOOD_RAIL = new Color(0xB9804A);

    /**
     * 박스 둘레에 나무 울타리를 그린다. 기둥을 일정한 간격으로 세우고 가로대로 잇는다.
     *
     * @param cap 기둥 머리에 칠할 색. 로그인한 사람의 울타리만 색을 칠하고 깃발을 단다(다른 사람은 null).
     */
    private void paintFence(Graphics2D g, int x, int y, int w, int h, Color cap) {
        int nx = Math.max(2, Math.round(w / 24f));
        int ny = Math.max(2, Math.round(h / 26f));
        rail(g, x, y - 5, x + w, y - 5);
        rail(g, x, y + 2, x + w, y + 2);
        for (int i = 1; i < nx; i++) {
            post(g, x + i * w / nx, y, 8, 18, cap);
        }
        rail(g, x, y, x, y + h);
        rail(g, x + w, y, x + w, y + h);
        if (cap != null) {
            paintFlag(g, x, y - 12, cap);
        }
        post(g, x, y, 10, 22, cap);
        post(g, x + w, y, 10, 22, cap);
        for (int i = 1; i < ny; i++) {
            post(g, x, y + i * h / ny, 8, 18, cap);
            post(g, x + w, y + i * h / ny, 8, 18, cap);
        }
        rail(g, x, y + h - 5, x + w, y + h - 5);
        rail(g, x, y + h + 2, x + w, y + h + 2);
        for (int i = 0; i <= nx; i++) {
            boolean corner = i == 0 || i == nx;
            post(g, x + i * w / nx, y + h, corner ? 10 : 8, corner ? 22 : 18, cap);
        }
    }

    private static void rail(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setStroke(new BasicStroke(5.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(UiKit.BROWN);
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(WOOD_RAIL);
        g.drawLine(x1, y1, x2, y2);
    }

    /** 기둥 하나. (cx, cy)는 울타리 선 위의 점이고 기둥은 그 점을 가운데로 선다. */
    private static void post(Graphics2D g, int cx, int cy, int pw, int ph, Color cap) {
        int left = cx - pw / 2;
        int top = cy - ph * 3 / 5;
        g.setColor(WOOD);
        g.fillRoundRect(left, top, pw, ph, 5, 5);
        g.setColor(WOOD_LIGHT);
        g.fillRect(left + 2, top + 3, 2, ph - 6);
        if (cap != null) {
            g.setColor(cap);
            g.fillRoundRect(left, top, pw, 7, 5, 5);
            g.setColor(UiKit.mix(cap, Color.WHITE, 0.55));
            g.fillRect(left + 2, top + 2, 2, 3);
        }
        g.setColor(UiKit.BROWN);
        g.setStroke(new BasicStroke(1.3f));
        g.drawRoundRect(left, top, pw, ph, 5, 5);
    }

    /** 내 울타리 왼쪽 위 모서리에 다는 깃발. 바람에 조금씩 흔들린다. */
    private void paintFlag(Graphics2D g, int px, int base, Color c) {
        int top = base - 20;
        g.setColor(UiKit.BROWN);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(px, base, px, top);
        int wave = (int) Math.round(Math.sin(tick * 0.22) * 2);
        int[] xs = {px + 1, px + 19, px + 1};
        int[] ys = {top, top + 6 + wave, top + 12};
        g.setColor(c);
        g.fillPolygon(xs, ys, 3);
        g.setColor(UiKit.mix(c, UiKit.INK, 0.35));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(xs, ys, 3);
    }

    private void paintInside(Graphics2D g, Member m, List<Task> in, int x, int y, int w, int h) {
        if (in.isEmpty()) {
            return;
        }
        Color wool = UiKit.mix(new Color(m.getColor()), Color.WHITE, 0.62);
        // 양 크기는 마릿수와 상관없이 같다. 자리가 모자라면 크기를 줄이지 않고 간격만 좁힌다.
        int size = SHEEP_SIZE;
        int cols = 1;
        double best = -1;
        for (int k = 1; k <= in.size(); k++) {
            int r = (in.size() + k - 1) / k;
            double fit = Math.min(Math.min(1, (w - 20) / (k * 74.0)), Math.min(1, (h - 22) / (r * 66.0)));
            if (fit >= best) { // 같은 조건이면 한 줄에 더 많이 세운다.
                best = fit;
                cols = k;
            }
        }
        int rowsN = (in.size() + cols - 1) / cols;
        int stepY = Math.min(72, Math.max(14, (h - 22) / rowsN));
        int stepX = Math.min(88, Math.max(20, (w - 20) / cols));
        int top = y + 18 + Math.max(0, (h - 22 - rowsN * stepY) / 2);
        for (int k = 0; k < in.size(); k++) {
            int r = k / cols;
            int inRow = r == rowsN - 1 ? in.size() - r * cols : cols;
            int cx = x + w / 2 - (inRow - 1) * stepX / 2 + (k % cols) * stepX;
            int cy = top + r * stepY + size / 2;
            sheepWithLabel(g, in.get(k), cx, cy, size, wool, false, stepX - 4);
        }
    }

    private void paintOutside(Graphics2D g, List<Task> out, int cellX, int cy, int cw) {
        if (out.isEmpty()) {
            return;
        }
        int stepX = Math.min(84, (cw - 30) / out.size());
        int start = cellX + cw / 2 - (out.size() - 1) * stepX / 2;
        for (int k = 0; k < out.size(); k++) {
            sheepWithLabel(g, out.get(k), start + k * stepX, cy, 38, new Color(0xFFFDF6), true, stepX - 4);
        }
    }

    private void sheepWithLabel(Graphics2D g, Task t, int x0, int y0, int size, Color wool, boolean faded,
                                int labelW) {
        // 양마다 박자가 다르다. 한 박자(28초쯤) 안에서 걷기 → 쉬기 → 풀 뜯기 → 쉬기를 되풀이한다.
        int seed = t.getId().hashCode() * 37;
        int time = tick + Math.floorMod(seed, CYCLE);
        int cycle = time / CYCLE;
        int u = time % CYCLE;
        int[] from = spot(seed, cycle);
        int[] to = spot(seed, cycle + 1);
        double k = Math.min(1.0, u / (double) WALK_TICKS);
        int cx = x0 + (int) Math.round(from[0] + (to[0] - from[0]) * k);
        int cy = y0 + (int) Math.round(from[1] + (to[1] - from[1]) * k);
        boolean selected = t.getId().equals(selectedId);
        int frame = Sprites.IDLE + ((tick / 4 + Math.floorMod(seed, 40)) % 40 == 0 ? 2 : 0);
        if (selected) {
            frame = Sprites.HAPPY + (tick / 4) % Sprites.COLS; // 고른 양은 깡충 뛴다.
        } else if (u < WALK_TICKS) {
            frame = Sprites.WALK + (tick / 4) % Sprites.COLS;
        } else if (u > 220 && u < 330) {
            frame = Sprites.WORK + (tick / 8) % Sprites.COLS;
        }
        if (selected) {
            g.setColor(new Color(255, 255, 255, 170));
            g.fillOval(cx - size / 2 - 9, cy - size / 2 - 7, size + 18, size + 18);
            g.setColor(UiKit.BROWN);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(cx - size / 2 - 9, cy - size / 2 - 7, size + 18, size + 18);
        }
        Sprites.sheep(g, cx, cy, size, wool, faded, frame, to[0] < from[0]);
        g.setFont(UiKit.font(Font.PLAIN, 11));
        UiKit.pill(g, t.getTitle(), cx, cy + size / 2 + 3, Math.max(40, labelW), UiKit.INK);
        hits.put(new Rectangle(cx - size / 2 - 4, cy - size / 2 - 4, size + 8, size + 26), t);
    }

    private static final int CYCLE = 400;
    private static final int WALK_TICKS = 70;

    /** 양이 한 박자마다 옮겨 가는 자리(원래 자리에서 벗어난 거리). 같은 양, 같은 박자면 늘 같은 값이다. */
    private static int[] spot(int seed, int cycle) {
        Random r = new Random(seed * 31L + cycle);
        return new int[]{r.nextInt(25) - 12, r.nextInt(9) - 4};
    }
}
