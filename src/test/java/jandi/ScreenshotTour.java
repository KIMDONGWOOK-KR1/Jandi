package jandi;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.Ranch;
import jandi.store.DataStore;
import jandi.store.SampleData;
import jandi.ui.LoginDialog;
import jandi.ui.MainFrame;
import jandi.ui.UiKit;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 보고서용 화면 캡처. 샘플 데이터로 화면을 띄우지 않고 그려서 PNG로 저장한다.
 * 실행: {@code java -cp build/classes:build/test-classes jandi.ScreenshotTour <출력 폴더>}
 */
public final class ScreenshotTour {

    private ScreenshotTour() {
    }

    public static void main(String[] args) throws Exception {
        Path out = Paths.get(args.length > 0 ? args[0] : "docs/images");
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("jandi-shot");
        SwingUtilities.invokeAndWait(() -> {
            try {
                UiKit.install();
                DataStore store = DataStore.open(data);
                SampleData.populateTasks(store);
                Path repo = data.resolve("sample-repo");
                if (SampleData.createRepo(store, repo)) {
                    store.setSetting("repoPath", repo.toString());
                }
                Ranch ranch = new Ranch(store);
                LoginDialog login = new LoginDialog(null, ranch);
                shot(login.getContentPane(), out.resolve("01_login.png"));
                login.dispose();

                Member me = ranch.members().get(2);
                MainFrame frame = new MainFrame(ranch, me, () -> { });
                frame.addNotify();
                frame.setSize(1400, 880);
                frame.validate();
                shot(frame.getContentPane(), out.resolve("02_ranch.png"));
                Task done = ranch.tasks().tasksOf(me.getId()).get(2);
                click(frame.getContentPane(), "목장"); // 선택 표시 확인용
                ranch.tasks().submit(me.getId(), done.getId());
                frame.refresh();
                frame.validate();
                shot(frame.getContentPane(), out.resolve("03_submitted.png"));
                click(frame.getContentPane(), "양털");
                frame.validate();
                shot(frame.getContentPane(), out.resolve("04_grass.png"));
                frame.dispose();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        System.out.println("저장: " + out.toAbsolutePath());
        System.exit(0);
    }

    private static void click(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton && text.equals(((AbstractButton) c).getText())) {
                ((AbstractButton) c).doClick(0);
            } else if (c instanceof Container) {
                click((Container) c, text);
            }
        }
    }

    private static void shot(Container c, Path file) throws Exception {
        if (c.getWidth() == 0) {
            c.setSize(c.getPreferredSize());
            layout(c);
        }
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", file.toFile());
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component k : c.getComponents()) {
            if (k instanceof Container) {
                layout((Container) k);
            }
        }
    }
}
