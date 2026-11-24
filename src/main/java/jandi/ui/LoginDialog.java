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
 * <p>모달 대화상자이다. 부르는 쪽({@code App})은 {@code setVisible(true)}로 띄우고, 창이 닫힌 뒤
 * {@link #getMember()}로 고른 팀원을 꺼낸다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public class LoginDialog extends JDialog {

    /** 고른 팀원. 아무도 고르지 않고 창을 닫으면 null로 남는다. */
    private Member chosen;

    /**
     * 팀원마다 버튼을 하나씩 만들어 한 줄로 놓는다.
     *
     * @param owner 부모 창(없으면 null)
     * @param ranch 목장
     */
    public LoginDialog(Frame owner, Ranch ranch) {
        super(owner, "잔디 — 로그인", true); // 세 번째 인자 true = 모달(닫힐 때까지 setVisible(true)가 돌아오지 않는다)
        // 잔디 바탕(Meadow) 위에 카드 한 장을 놓고, 카드 위쪽에 안내 글 두 줄, 가운데에 팀원 버튼들을 둔다.
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
        // GridLayout(1, 0)은 한 줄에 넣는 만큼 칸이 늘어난다. 팀원 수가 바뀌어도 같은 크기의 버튼이 나란히 놓인다.
        JPanel row = UiKit.clear(new GridLayout(1, 0, 12, 0));
        for (Member m : ranch.members()) {
            row.add(new FarmerButton(m));
        }
        card.add(row, BorderLayout.CENTER);
        root.add(card);
        setContentPane(root);
        pack(); // 내용물의 선호 크기에 맞춰 창 크기를 정한다.
        setResizable(false);
        setLocationRelativeTo(owner); // owner가 null이면 화면 가운데에 뜬다.
    }

    /** @return 고른 팀원(창을 그냥 닫았으면 null) */
    public Member getMember() {
        return chosen;
    }

    /**
     * 팀원 한 명을 고르는 버튼. JButton의 기본 모양을 끄고 농부 그림과 이름을 직접 그린다.
     * 모양만 바꾼 것이라, 눌림 처리와 마우스 올림(rollover) 같은 버튼 동작은 JButton의 것을 그대로 쓴다.
     */
    private final class FarmerButton extends JButton {
        private final Member member;

        FarmerButton(Member m) {
            this.member = m;
            setPreferredSize(new Dimension(118, 150));
            // 직접 그릴 것이므로 기본 배경, 테두리, 포커스 표시를 끈다.
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true); // 마우스가 올라왔는지 알 수 있게 한다(그릴 때 바탕색을 바꾼다).
            addActionListener(e -> {
                chosen = member; // 바깥 클래스의 필드에 고른 팀원을 적고
                dispose();       // 창을 닫는다. 그러면 App의 setVisible(true)가 돌아온다.
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UiKit.aa(g0.create());
            Color c = new Color(member.getColor());
            // 마우스가 올라오면 팀원 색을 옅게 섞은 바탕, 아니면 기본 옅은 바탕
            g.setColor(getModel().isRollover() ? UiKit.mix(c, Color.WHITE, 0.7) : UiKit.SOFT);
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 16, 16);
            Sprites.farmer(g, getWidth() / 2 - 4, 106, 84, c, Sprites.IDLE, false); // 팀원 색 옷을 입고 서 있는 농부
            g.setFont(UiKit.font(Font.BOLD, 15));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(UiKit.INK);
            g.drawString(member.getName(), (getWidth() - fm.stringWidth(member.getName())) / 2, 134);
            g.dispose();
        }
    }
}
