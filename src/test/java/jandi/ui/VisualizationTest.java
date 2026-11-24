package jandi.ui;

import jandi.domain.Contribution;

import java.time.LocalDate;
import java.util.List;

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

    private static void check(boolean ok) {
        if (!ok) {
            throw new AssertionError();
        }
    }
}
