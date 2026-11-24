package jandi.ui;

import jandi.domain.Contribution;
import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.Ranch;
import jandi.store.DataStore;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SelfTest에서 실행하는 기여도 시각화 회귀 테스트. */
public final class VisualizationTest {
    private VisualizationTest() {
    }

    public static void percentages() {
        check(UiKit.contributionText(false, 0).equals("–"));
        check(UiKit.contributionText(true, 0).equals("0%"));
        for (double percent : new double[]{0.01, 0.4, 0.99}) {
            check(UiKit.contributionText(true, percent).equals("<1%"));
        }
        check(UiKit.contributionText(true, 1).equals("1%"));
        check(UiKit.contributionText(true, 12.6).equals("13%"));
        check(UiKit.contributionText(true, 100).equals("100%"));
    }

    public static void grassWindow() {
        LocalDate start = LocalDate.of(2026, 6, 14);
        LocalDate today = start.plusWeeks(23).plusDays(2);
        Contribution a = new Contribution("a");
        Contribution b = new Contribution("b");
        List<Contribution> team = List.of(a, b);
        a.getDaily().put(start.minusDays(1), 100);
        b.getDaily().put(today.plusDays(1), 200);
        check(GrassPanel.visibleMax(team, start, today) == 0);
        a.getDaily().put(start, 1);
        check(GrassPanel.visibleMax(team, start, today) == 1);
        b.getDaily().put(today, 8);
        int max = GrassPanel.visibleMax(team, start, today);
        check(max == 8 && GrassPanel.level(1, max) == 1 && GrassPanel.level(8, max) == 4);
    }

    public static void submittedSheep() throws Exception {
        Ranch ranch = new Ranch(DataStore.open(Files.createTempDirectory("jandi-small-pen"))) {
            @Override public boolean isGitReady() { return true; }
        };
        Member me = ranch.members().get(3);
        ranch.contributionOf(ranch.members().get(0).getId()).setPercent(100);
        SwingUtilities.invokeAndWait(() -> {
            Set<String> clicked = new HashSet<>();
            RanchPanel panel = new RanchPanel(ranch, me, t -> clicked.add(t.getTitle()));
            // 최소 창(1180×760)에서 확인한 목장 패널과 오른쪽 아래 0% 울타리.
            panel.setSize(488, 455);
            Rectangle pen = new Rectangle(320, 285, 91, 40);
            BufferedImage empty = render(panel);
            Set<String> titles = new HashSet<>();
            for (int i = 1; i <= 4; i++) {
                Task task = ranch.tasks().add(me.getId(), "제출 " + i, "");
                ranch.tasks().toggleDone(me.getId(), task.getId());
                ranch.tasks().submit(me.getId(), task.getId());
                titles.add(task.getTitle());
                panel.setSelected(task.getId());
                BufferedImage painted = render(panel);
                boolean visible = false;
                for (int y = 0; y < painted.getHeight(); y++) {
                    for (int x = 0; x < painted.getWidth(); x++) {
                        if (empty.getRGB(x, y) != painted.getRGB(x, y)) {
                            check(pen.contains(x, y));
                            visible = true;
                        }
                    }
                }
                check(visible);
                clicked.clear();
                for (int y = pen.y; y < pen.y + pen.height; y += 2) {
                    for (int x = pen.x; x < pen.x + pen.width; x += 2) {
                        MouseEvent click = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 1, false);
                        String title = panel.getToolTipText(click);
                        if (title != null && !clicked.contains(title)) {
                            panel.dispatchEvent(click);
                            check(clicked.contains(title));
                        }
                    }
                }
                check(clicked.equals(titles));
            }
        });
    }

    private static BufferedImage render(RanchPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image;
    }

    private static void check(boolean ok) {
        if (!ok) {
            throw new AssertionError();
        }
    }
}
