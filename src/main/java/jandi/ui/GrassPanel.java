package jandi.ui;

import jandi.domain.Contribution;
import jandi.domain.Member;
import jandi.service.Ranch;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.List;

/**
 * 양털 화면(잔디밭). 팀원별로 날짜마다 커밋 수를 색의 진하기로 보여 준다.
 *
 * @author Team 3
 * @version 0.1
 */
public class GrassPanel extends UiKit.Card {

    private static final int WEEKS = 24;
    private static final Color EMPTY = new Color(0xE9E2CF);
    private final Ranch ranch;

    /** @param ranch 목장 */
    public GrassPanel(Ranch ranch) {
        this.ranch = ranch;
    }

    /**
     * 하루 커밋 수를 진하기 단계로 바꾼다.
     *
     * @param count 그날 커밋 수
     * @param max   하루 최대 커밋 수
     * @return 0(없음)~4(가장 진함)
     */
    public static int level(int count, int max) {
        if (count <= 0 || max <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(4, (int) Math.ceil(4.0 * count / max)));
    }

    /** 같은 화면의 팀원들은 표시 기간 안에서 가장 많은 하루 커밋 수를 색 기준으로 공유한다. */
    static int visibleMax(Collection<Contribution> team, LocalDate start, LocalDate today) {
        int max = 0;
        for (Contribution member : team) {
            for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
                max = Math.max(max, member.getDaily().getOrDefault(day, 0));
            }
        }
        return max;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = UiKit.aa(g0.create());
        int w = getWidth();
        int h = getHeight();
        g.setFont(UiKit.font(Font.BOLD, 18));
        g.setColor(UiKit.INK);
        g.drawString("양털 — 날짜별 커밋 잔디밭", 24, 40);
        g.setFont(UiKit.font(Font.PLAIN, 12));
        g.setColor(UiKit.MUTED);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(UiKit.fit(fm, ranch.gitMessage(), w - 48), 24, 60);

        List<Member> ms = ranch.members();
        boolean hasTeamCommits = ranch.contributions().values().stream().anyMatch(c -> c.getCommits() > 0);
        int left = 190;
        int top = 84;
        int rowH = Math.max(60, (h - top - 44) / Math.max(1, ms.size()));
        int cell = Math.max(5, Math.min(18, Math.min((w - left - 30) / WEEKS, (rowH - 16) / 7)));
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).minusWeeks(WEEKS - 1L);
        int max = visibleMax(ranch.contributions().values(), start, today);
        for (int i = 0; i < ms.size(); i++) {
            Member m = ms.get(i);
            Contribution c = ranch.contributionOf(m.getId());
            Color base = new Color(m.getColor());
            int y = top + i * rowH;
            Sprites.farmer(g, 46, y + 52, 46, base, Sprites.IDLE, false);
            g.setFont(UiKit.font(Font.BOLD, 14));
            g.setColor(UiKit.INK);
            String pct = UiKit.contributionText(hasTeamCommits, c.getPercent());
            g.drawString(m.getName() + " · " + pct, 78, y + 22);
            g.setFont(UiKit.font(Font.PLAIN, 13));
            g.setColor(UiKit.MUTED);
            g.drawString("커밋 " + c.getCommits() + "개", 78, y + 39);
            g.drawString("+" + c.getAdded() + " / -" + c.getDeleted() + " 줄", 78, y + 54);
            for (int wk = 0; wk < WEEKS; wk++) {
                for (int d = 0; d < 7; d++) {
                    LocalDate day = start.plusDays(wk * 7L + d);
                    if (day.isAfter(today)) {
                        continue;
                    }
                    int lv = level(c.getDaily().getOrDefault(day, 0), max);
                    g.setColor(lv == 0 ? EMPTY : UiKit.mix(UiKit.mix(base, Color.WHITE, 0.75), base, (lv - 1) / 3.0));
                    g.fillRoundRect(left + wk * cell, y + 4 + d * cell, cell - 3, cell - 3, 4, 4);
                }
            }
        }
        // 범례
        g.setFont(UiKit.font(Font.PLAIN, 13));
        g.setColor(UiKit.MUTED);
        int ly = h - 34;
        g.drawString("최근 " + WEEKS + "주 · 적음", left, ly + 10);
        int lx = left + g.getFontMetrics().stringWidth("최근 " + WEEKS + "주 · 적음") + 8;
        for (int lv = 0; lv <= 4; lv++) {
            g.setColor(lv == 0 ? EMPTY : UiKit.mix(new Color(0xD6EDC4), UiKit.GREEN, (lv - 1) / 3.0));
            g.fillRoundRect(lx + lv * 16, ly, 12, 12, 4, 4);
        }
        g.setColor(UiKit.MUTED);
        g.drawString("많음", lx + 5 * 16 + 4, ly + 10);
        g.dispose();
    }
}
