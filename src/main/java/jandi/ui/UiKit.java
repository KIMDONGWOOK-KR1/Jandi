package jandi.ui;

import jandi.domain.Task;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 화면 공통 도구: 색, 글꼴, 카드 패널, 둥근 버튼.
 *
 * @author Team 3
 * @version 0.1
 */
public final class UiKit {

    /** 잔디 바탕색. */
    public static final Color GRASS = new Color(0xB5DB95);
    /** 잔디 무늬색. */
    public static final Color GRASS_DARK = new Color(0x86BC68);
    /** 박스 안쪽 색. */
    public static final Color PEN = new Color(0xD0EAB6);
    /** 카드 바탕색. */
    public static final Color CREAM = new Color(0xFFF8E8);
    /** 카드 안의 옅은 칸 색. */
    public static final Color SOFT = new Color(0xF0E6D2);
    /** 테두리·단추 갈색. */
    public static final Color BROWN = new Color(0x8A5A2B);
    /** 글자색. */
    public static final Color INK = new Color(0x3E2A17);
    /** 흐린 글자색. */
    public static final Color MUTED = new Color(0x8C7A66);
    /** 완료 표시 초록. */
    public static final Color GREEN = new Color(0x3FA34D);

    private static String family = Font.SANS_SERIF;

    private UiKit() {
    }

    /** 글꼴을 고르고 기본 대화상자 글꼴을 맞춘다. */
    public static void install() {
        List<String> have = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[]{"Apple SD Gothic Neo", "Malgun Gothic", "맑은 고딕", "NanumGothic",
                "Noto Sans CJK KR"}) {
            if (have.contains(f)) {
                family = f;
                break;
            }
        }
        Font base = font(Font.PLAIN, 13);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "TextArea.font",
                "ComboBox.font", "OptionPane.messageFont", "OptionPane.buttonFont", "ToolTip.font"}) {
            UIManager.put(key, base);
        }
    }

    /**
     * @param style 글꼴 모양({@link Font#PLAIN}, {@link Font#BOLD})
     * @param size  크기
     * @return 글꼴
     */
    public static Font font(int style, float size) {
        return new Font(family, style, Math.round(size));
    }

    /**
     * @param g 그래픽
     * @return 매끄럽게 그리도록 설정한 그래픽
     */
    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * 두 색을 섞는다.
     *
     * @param a 첫 색
     * @param b 둘째 색
     * @param t 둘째 색의 비율(0~1)
     * @return 섞은 색
     */
    public static Color mix(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /**
     * 너비에 맞게 글을 줄인다.
     *
     * @param fm 글꼴 정보
     * @param s  글
     * @param w  최대 너비
     * @return 너비를 넘으면 끝을 …로 줄인 글
     */
    public static String fit(FontMetrics fm, String s, int w) {
        if (fm.stringWidth(s) <= w) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > w) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    /**
     * 가운데 맞춘 알약 모양 이름표를 그린다.
     *
     * @param g    그래픽
     * @param text 글
     * @param cx   가운데 x
     * @param y    위쪽 y
     * @param maxW 최대 너비
     * @param fg   글자색
     */
    public static void pill(Graphics2D g, String text, int cx, int y, int maxW, Color fg) {
        FontMetrics fm = g.getFontMetrics();
        String t = fit(fm, text, maxW - 10);
        int w = fm.stringWidth(t) + 10;
        int h = fm.getHeight() + 2;
        g.setColor(new Color(255, 255, 255, 215));
        g.fillRoundRect(cx - w / 2, y, w, h, 8, 8);
        g.setColor(fg);
        g.drawString(t, cx - w / 2 + 5, y + fm.getAscent() + 1);
    }

    /**
     * 잔디 바탕과 풀·꽃 무늬를 그린다.
     *
     * @param g 그래픽
     * @param w 너비
     * @param h 높이
     */
    public static void paintGrass(Graphics2D g, int w, int h) {
        g.setColor(GRASS);
        g.fillRect(0, 0, w, h);
        Random r = new Random(7);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 46; i++) {
            int x = r.nextInt(Math.max(1, w));
            int y = r.nextInt(Math.max(1, h));
            g.setColor(GRASS_DARK);
            g.drawLine(x, y, x - 5, y - 10);
            g.drawLine(x, y, x, y - 13);
            g.drawLine(x, y, x + 6, y - 9);
        }
        for (int i = 0; i < 14; i++) {
            int x = r.nextInt(Math.max(1, w));
            int y = r.nextInt(Math.max(1, h));
            g.setColor(Color.WHITE);
            for (int k = 0; k < 4; k++) {
                g.fillOval(x + (k % 2) * 6 - 5, y + (k / 2) * 6 - 5, 7, 7);
            }
            g.setColor(new Color(0xF6C445));
            g.fillOval(x - 2, y - 2, 5, 5);
        }
    }

    /**
     * 직접 그린 양을 마우스로 고를 수 있게 한다. 양 위에서는 손 모양 커서를 보이고, 누르면 그 할 일을 넘긴다.
     *
     * @param c        양을 그리는 패널
     * @param hits     그릴 때 채워 두는 클릭 영역과 할 일 (그린 순서대로)
     * @param onSelect 양을 눌렀을 때 부를 동작
     */
    static void listenTaskClicks(JComponent c, Map<Rectangle, Task> hits, Consumer<Task> onSelect) {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Task t = taskAt(hits, e);
                if (t != null) {
                    onSelect.accept(t);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                c.setCursor(Cursor.getPredefinedCursor(taskAt(hits, e) != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        c.addMouseListener(mouse);
        c.addMouseMotionListener(mouse);
    }

    /** 마우스 위치의 할 일. 겹치면 나중에 그린(위에 있는) 양이 이긴다. */
    private static Task taskAt(Map<Rectangle, Task> hits, MouseEvent e) {
        Task found = null;
        for (Map.Entry<Rectangle, Task> h : hits.entrySet()) {
            if (h.getKey().contains(e.getPoint())) {
                found = h.getValue();
            }
        }
        return found;
    }

    /**
     * 크림색 바탕에 갈색 테두리와 그림자가 있는 둥근 카드 패널.
     */
    public static class Card extends JPanel {
        private static final long serialVersionUID = 1L;

        /** 카드를 만든다. */
        public Card() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 20, 16));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0.create());
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(94, 59, 26, 70));
            g.fillRoundRect(2, 6, w - 4, h - 7, 20, 20);
            g.setColor(CREAM);
            g.fillRoundRect(2, 2, w - 4, h - 8, 18, 18);
            g.setColor(BROWN);
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(2, 2, w - 4, h - 8, 18, 18);
            g.dispose();
        }
    }

    /**
     * 아래쪽에 그림자가 있는 둥근 버튼.
     */
    public static class RoundButton extends JButton {
        private static final long serialVersionUID = 1L;
        private Color fill;
        private Color text;

        /**
         * @param label   버튼 글
         * @param primary true면 갈색 강조 버튼, false면 옅은 버튼
         */
        public RoundButton(String label, boolean primary) {
            super(label);
            setPrimary(primary);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(font(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 11, 14));
        }

        /** @param primary true면 갈색 강조 버튼으로 바꾼다 */
        public final void setPrimary(boolean primary) {
            fill = primary ? BROWN : new Color(0xE9DCC3);
            text = primary ? Color.WHITE : BROWN;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = aa(g0.create());
            int w = getWidth();
            int h = getHeight();
            boolean down = getModel().isPressed() && getModel().isArmed();
            Color bg = isEnabled() ? fill : mix(fill, CREAM, 0.55);
            g.setColor(mix(bg, Color.BLACK, 0.28));
            g.fillRoundRect(0, 3, w, h - 3, 14, 14);
            g.setColor(bg);
            g.fillRoundRect(0, down ? 2 : 0, w, h - 3, 14, 14);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(isEnabled() ? text : mix(text, bg, 0.5));
            String s = fit(fm, getText(), w - 8);
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - 3 - fm.getHeight()) / 2 + fm.getAscent() + (down ? 2 : 0));
            g.dispose();
        }
    }
}
