package jandi.ui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * 양과 농부를 그린다. 그림은 스프라이트 시트({@code /jandi/farmer.png}, {@code /jandi/sheep.png})에서 잘라 쓴다.
 *
 * <p>시트는 가로 4칸, 세로 4줄이다. 농부는 줄마다 서 있기·걷기·물 주기·기뻐하기,
 * 양은 서 있기·걷기·풀 뜯기·깡충 뛰기 동작이다. 원본의 보라색 부분은 팀원 색으로 바꿔 칠한다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public final class Sprites {

    /** 한 줄의 칸 수. */
    public static final int COLS = 4;
    /** 서 있는 동작의 첫 칸. */
    public static final int IDLE = 0;
    /** 걷는 동작의 첫 칸. */
    public static final int WALK = 4;
    /** 농부는 물 주기, 양은 풀 뜯기 동작의 첫 칸. */
    public static final int WORK = 8;
    /** 농부는 기뻐하기, 양은 깡충 뛰기 동작의 첫 칸. */
    public static final int HAPPY = 12;

    private static final Sheet FARMER = new Sheet("/jandi/farmer.png");
    private static final Sheet SHEEP = new Sheet("/jandi/sheep.png");

    private Sprites() {
    }

    /**
     * 양을 서 있는 모습으로 그린다.
     *
     * @param g     그래픽
     * @param cx    가운데 x
     * @param cy    가운데 y
     * @param s     크기(키쯤)
     * @param wool  양털 색(흰색에 가까우면 원래 색 그대로 그린다)
     * @param faded true면 흐리게 그린다(아직 제출하지 않은 양)
     */
    public static void sheep(Graphics2D g, int cx, int cy, int s, Color wool, boolean faded) {
        sheep(g, cx, cy, s, wool, faded, IDLE, false);
    }

    /**
     * 양을 그린다.
     *
     * @param g     그래픽
     * @param cx    가운데 x
     * @param cy    가운데 y
     * @param s     크기(키쯤)
     * @param wool  양털 색(흰색에 가까우면 원래 색 그대로 그린다)
     * @param faded true면 흐리게 그린다(아직 제출하지 않은 양)
     * @param frame 동작 칸(0~15)
     * @param flip  true면 좌우를 뒤집는다
     */
    public static void sheep(Graphics2D g, int cx, int cy, int s, Color wool, boolean faded, int frame,
                             boolean flip) {
        Composite old = g.getComposite();
        if (faded) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.62f));
        }
        int h = (int) Math.round(s * 1.3);
        int foot = cy + (int) Math.round(s * 0.62);
        if (!SHEEP.draw(g, cx, foot, h, wool, frame, flip, true)) {
            g.setColor(wool);
            g.fillOval(cx - s / 2, cy - s / 2, s, s);
        }
        g.setComposite(old);
    }

    /**
     * 농부를 서 있는 모습으로 그린다.
     *
     * @param g     그래픽
     * @param cx    가운데 x
     * @param footY 발끝 y
     * @param h     키
     * @param shirt 옷 색(팀원 대표 색)
     */
    public static void farmer(Graphics2D g, int cx, int footY, int h, Color shirt) {
        farmer(g, cx, footY, h, shirt, IDLE, false);
    }

    /**
     * 농부를 그린다.
     *
     * @param g     그래픽
     * @param cx    가운데 x
     * @param footY 발끝 y
     * @param h     키
     * @param shirt 옷 색(팀원 대표 색)
     * @param frame 동작 칸(0~15). {@link #IDLE}, {@link #WALK}, {@link #WORK}, {@link #HAPPY}에 0~3을 더한다
     * @param flip  true면 좌우를 뒤집어 왼쪽을 보게 한다
     */
    public static void farmer(Graphics2D g, int cx, int footY, int h, Color shirt, int frame, boolean flip) {
        if (!FARMER.draw(g, cx, footY, h, shirt, frame, flip, false)) {
            g.setColor(shirt);
            g.fillRoundRect(cx - h / 5, footY - h, h * 2 / 5, h, 10, 10);
        }
    }

    /**
     * 농부 그림의 보라색 계열(옷, 모자 띠)을 팀원 색으로 바꾼다. 음영은 남기고 조금 더 선명하게 칠한다.
     *
     * @param argb  원래 색
     * @param shirt 바꿀 색
     * @return 바꾼 색(보라색 계열이 아니면 그대로)
     */
    public static int recolor(int argb, Color shirt) {
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
        if (hsb[0] < 0.66f || hsb[0] > 0.84f || hsb[1] < 0.15f) {
            return argb;
        }
        float[] to = Color.RGBtoHSB(shirt.getRed(), shirt.getGreen(), shirt.getBlue(), null);
        int rgb = Color.HSBtoRGB(to[0], Math.min(0.95f, hsb[1] * 2.3f), Math.min(1f, hsb[2] * 1.18f));
        return (argb & 0xFF000000) | (rgb & 0xFFFFFF);
    }

    /**
     * 양 그림의 털을 팀원 색으로 물들이고, 보라색 목줄도 같은 계열로 바꾼다. 얼굴과 방울은 그대로 둔다.
     *
     * @param argb 원래 색
     * @param wool 양털 색(옅은 색). 흰색에 가까우면 아무것도 바꾸지 않는다
     * @return 바꾼 색
     */
    public static int tintWool(int argb, Color wool) {
        float[] to = Color.RGBtoHSB(wool.getRed(), wool.getGreen(), wool.getBlue(), null);
        if (to[1] < 0.08f) {
            return argb;
        }
        int r = (argb >> 16) & 255;
        int gr = (argb >> 8) & 255;
        int b = argb & 255;
        float[] hsb = Color.RGBtoHSB(r, gr, b, null);
        boolean purple = hsb[0] >= 0.6f && hsb[0] <= 0.9f;
        if (purple && hsb[1] >= 0.28f) { // 목줄
            int rgb = Color.HSBtoRGB(to[0], 0.7f, Math.min(1f, hsb[2] * 1.1f));
            return (argb & 0xFF000000) | (rgb & 0xFFFFFF);
        }
        if (hsb[2] > 0.6f && (hsb[1] < 0.2f || purple)) { // 털과 털의 그늘
            // 털의 밝기(그늘)만 남기고 양털 색을 입힌다. 그늘이 탁해지지 않게 밝기를 조금 끌어올린다.
            double light = Math.min(1.0, (0.6 + 0.4 * hsb[2]) * (purple ? 0.93 : 1.0) + 0.03);
            return (argb & 0xFF000000) | ((int) (wool.getRed() * light) << 16)
                    | ((int) (wool.getGreen() * light) << 8) | (int) (wool.getBlue() * light);
        }
        return argb;
    }

    /** 스프라이트 시트 한 장. 처음 쓸 때 읽어서 칸으로 자르고, 크기·색별로 줄인 그림을 기억해 둔다. */
    private static final class Sheet {
        private final String path;
        private final int[] baseline = new int[4];
        private final Map<String, BufferedImage> cache = new HashMap<>();
        private BufferedImage[] frames;
        private boolean tried;
        private int cell;
        private int charHeight;

        Sheet(String path) {
            this.path = path;
        }

        synchronized boolean draw(Graphics2D g, int cx, int footY, int h, Color color, int frame, boolean flip,
                                  boolean wool) {
            load();
            if (frames == null) {
                return false;
            }
            double scale = h / (double) charHeight;
            int size = (int) Math.round(cell * scale);
            int f = Math.floorMod(frame, frames.length);
            Composite keep = g.getComposite();
            g.setColor(new Color(60, 90, 40, 60));
            g.fillOval(cx - (int) (h * 0.3), footY - (int) (h * 0.07), (int) (h * 0.6), (int) (h * 0.13));
            g.setComposite(keep);
            Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(scaled(f, size, color, flip, wool), cx - size / 2,
                    footY - (int) Math.round(baseline[f / COLS] * scale), size, size, null);
            if (old != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
            }
            return true;
        }

        private void load() {
            if (tried) {
                return;
            }
            tried = true;
            try (InputStream in = Sprites.class.getResourceAsStream(path)) {
                if (in == null) {
                    return;
                }
                BufferedImage sheet = ImageIO.read(in);
                cell = sheet.getWidth() / COLS;
                BufferedImage[] fs = new BufferedImage[COLS * 4];
                int top = cell;
                for (int i = 0; i < fs.length; i++) {
                    BufferedImage f = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = f.createGraphics();
                    g.drawImage(sheet, -(i % COLS) * cell, -(i / COLS) * cell, null);
                    g.dispose();
                    clearBackground(f);
                    fs[i] = f;
                    int[] tb = topBottom(f);
                    baseline[i / COLS] = Math.max(baseline[i / COLS], tb[1]);
                    if (i < COLS) {
                        top = Math.min(top, tb[0]);
                    }
                }
                charHeight = Math.max(1, baseline[0] - top);
                frames = fs;
            } catch (Exception e) {
                frames = null;
            }
        }

        private BufferedImage scaled(int frame, int size, Color color, boolean flip, boolean wool) {
            String key = frame + ":" + size + ":" + color.getRGB() + ":" + flip;
            BufferedImage hit = cache.get(key);
            if (hit != null) {
                return hit;
            }
            BufferedImage src = frames[frame];
            BufferedImage colored = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    int p = src.getRGB(x, y);
                    colored.setRGB(flip ? cell - 1 - x : x, y,
                            (p >>> 24) == 0 ? 0 : wool ? tintWool(p, color) : recolor(p, color));
                }
            }
            int px = Math.max(1, size * 2); // 고해상도 화면에서도 또렷하도록 2배로 만들어 둔다.
            Image small = new ImageIcon(colored.getScaledInstance(px, px, Image.SCALE_AREA_AVERAGING)).getImage();
            BufferedImage out = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(small, 0, 0, null);
            g.dispose();
            cache.put(key, out);
            return out;
        }

        /** 가장자리에서 이어진 흰 바탕을 투명하게 만든다. 밝은 털과 옷은 테두리 안쪽이라 남는다. */
        private static void clearBackground(BufferedImage f) {
            int w = f.getWidth();
            int h = f.getHeight();
            boolean[] seen = new boolean[w * h];
            ArrayDeque<Integer> q = new ArrayDeque<>();
            for (int x = 0; x < w; x++) {
                q.add(x);
                q.add((h - 1) * w + x);
            }
            for (int y = 0; y < h; y++) {
                q.add(y * w);
                q.add(y * w + w - 1);
            }
            while (!q.isEmpty()) {
                int p = q.poll();
                if (seen[p]) {
                    continue;
                }
                seen[p] = true;
                int x = p % w;
                int y = p / w;
                int rgb = f.getRGB(x, y);
                if (((rgb >> 16) & 255) < 228 || ((rgb >> 8) & 255) < 228 || (rgb & 255) < 228) {
                    continue;
                }
                f.setRGB(x, y, 0);
                if (x > 0) {
                    q.add(p - 1);
                }
                if (x < w - 1) {
                    q.add(p + 1);
                }
                if (y > 0) {
                    q.add(p - w);
                }
                if (y < h - 1) {
                    q.add(p + w);
                }
            }
        }

        private static int[] topBottom(BufferedImage f) {
            int top = f.getHeight();
            int bottom = 0;
            for (int y = 0; y < f.getHeight(); y++) {
                int n = 0;
                for (int x = 0; x < f.getWidth(); x++) {
                    if ((f.getRGB(x, y) >>> 24) > 0) {
                        n++;
                    }
                }
                if (n >= 6) {
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
            return new int[]{top, bottom};
        }
    }
}
