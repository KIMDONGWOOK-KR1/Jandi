package jandi.ui;

import jandi.domain.Member;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

/**
 * 로그인 창. 지금은 이름을 골라 들어간다(비밀번호는 다음 단계에서 넣는다).
 *
 * @author Team 3
 * @version 0.1
 */
public class LoginDialog extends JDialog {

    private Member chosen;

    /**
     * @param owner 부모 창(없으면 null)
     * @param ranch 목장
     */
    public LoginDialog(Frame owner, Ranch ranch) {
        super(owner, "잔디 — 로그인", true);
        JPanel root = new UiKit.Meadow(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        UiKit.Card card = new UiKit.Card();
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 28, 24));
        JLabel title = UiKit.label("누구로 들어갈까요?", Font.BOLD, 20, UiKit.INK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = UiKit.label("잔디 — 팀 기여도 목장", Font.PLAIN, 13, UiKit.MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel head = UiKit.clear(new GridLayout(2, 1, 0, 2));
        head.add(title);
        head.add(sub);
        card.add(head, BorderLayout.NORTH);
        JPanel row = UiKit.clear(new GridLayout(1, 0, 12, 0));
        for (Member m : ranch.members()) {
            row.add(new FarmerButton(m));
        }
        card.add(row, BorderLayout.CENTER);
        root.add(card);
        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** @return 고른 팀원(창을 그냥 닫았으면 null) */
    public Member getMember() {
        return chosen;
    }

    private final class FarmerButton extends JButton {
        private final Member member;

        FarmerButton(Member m) {
            this.member = m;
            setPreferredSize(new Dimension(118, 150));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
            addActionListener(e -> {
                chosen = member;
                dispose();
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UiKit.aa(g0.create());
            Color c = new Color(member.getColor());
            g.setColor(getModel().isRollover() ? UiKit.mix(c, Color.WHITE, 0.7) : UiKit.SOFT);
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 16, 16);
            Sprites.farmer(g, getWidth() / 2 - 4, 106, 84, c, Sprites.IDLE, false);
            g.setFont(UiKit.font(Font.BOLD, 15));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(UiKit.INK);
            g.drawString(member.getName(), (getWidth() - fm.stringWidth(member.getName())) / 2, 134);
            g.dispose();
        }
    }
}
