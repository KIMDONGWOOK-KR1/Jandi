package jandi.ui;

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

    private static void check(boolean ok) {
        if (!ok) {
            throw new AssertionError();
        }
    }
}
