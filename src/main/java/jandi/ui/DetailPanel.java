package jandi.ui;

import jandi.domain.Member;
import jandi.domain.Task;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 오른쪽 상세 카드. 누른 양(할 일)의 내용을 보여 주고, 내 양이면 제출하거나 지울 수 있고, 박스 안의 내 양은 다시 내보낼 수 있다.
 *
 * @author Team 3
 * @version 0.1
 */
public class DetailPanel extends UiKit.Card {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private final Ranch ranch;
    private final Member me;
    private final JLabel title = UiKit.label("양을 눌러 보세요", Font.BOLD, 19, UiKit.INK);
    private final JLabel body = UiKit.label("", Font.PLAIN, 13, UiKit.INK);
    private final UiKit.RoundButton submit = new UiKit.RoundButton("양 들여보내기 (제출)", true);
    private final UiKit.RoundButton delete = new UiKit.RoundButton("삭제", false);
    private final UiKit.RoundButton withdraw = new UiKit.RoundButton("양 내보내기", false);
    private Task task;

    /**
     * @param ranch    목장
     * @param me       로그인한 팀원
     * @param onSubmit 제출을 눌렀을 때 부를 동작
     * @param onDelete 삭제를 눌렀을 때 부를 동작
     * @param onWithdraw 내보내기를 눌렀을 때 부를 동작
     */
    public DetailPanel(Ranch ranch, Member me, Consumer<Task> onSubmit, Consumer<Task> onDelete,
                       Consumer<Task> onWithdraw) {
        this.ranch = ranch;
        this.me = me;
        setLayout(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(310, 330));
        body.setVerticalAlignment(SwingConstants.TOP);
        submit.addActionListener(e -> onSubmit.accept(task));
        delete.addActionListener(e -> onDelete.accept(task));
        withdraw.addActionListener(e -> onWithdraw.accept(task));
        withdraw.setFont(UiKit.font(Font.BOLD, 11));
        withdraw.setBorder(BorderFactory.createEmptyBorder(4, 10, 7, 10));
        JPanel small = UiKit.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        small.add(withdraw);
        JPanel main = UiKit.clear(new GridLayout(0, 1, 0, 8));
        main.add(submit);
        main.add(delete);
        JPanel buttons = UiKit.clear(new BorderLayout(0, 6));
        buttons.add(small, BorderLayout.NORTH);
        buttons.add(main, BorderLayout.CENTER);
        add(title, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        show(null);
    }

    /**
     * 할 일을 보여 준다.
     *
     * @param t 보여 줄 할 일(null이면 안내 문구)
     */
    public final void show(Task t) {
        this.task = t;
        if (t == null) {
            title.setText("양을 눌러 보세요");
            body.setText(html("목장의 양이나 아래 팀원 줄의 양을 누르면 그 업무가 여기에 보여요.<br><br>"
                    + "박스 크기는 git 기여도에 비례해요."));
            submit.setEnabled(false);
            delete.setVisible(false);
            withdraw.setVisible(false);
            return;
        }
        boolean mine = t.getOwnerId().equals(me.getId());
        String owner = ranch.store().findMember(t.getOwnerId()).map(Member::getName).orElse("?");
        title.setText(t.getTitle());
        StringBuilder sb = new StringBuilder();
        sb.append("<b>담당</b> ").append(esc(owner)).append(mine ? " (나)" : "").append("<br>");
        sb.append("<b>상태</b> ").append(t.getStatus().label());
        if (t.getSubmittedAt() != null) {
            sb.append(" ").append(WHEN.format(t.getSubmittedAt()));
        }
        sb.append("<br><b>만든 날</b> ").append(WHEN.format(t.getCreatedAt())).append("<br><br>");
        sb.append("<b>한 일</b><br>").append(t.getMemo().isBlank() ? "<font color='#8C7A66'>메모가 없습니다.</font>"
                : esc(t.getMemo()).replace("\n", "<br>"));
        body.setText(html(sb.toString()));
        submit.setEnabled(mine && !t.isSubmitted());
        submit.setText(t.isSubmitted() ? "박스 안에 있어요" : mine ? "양 들여보내기 (제출)" : "다른 팀원의 양이에요");
        delete.setVisible(mine && !t.isSubmitted());
        withdraw.setVisible(mine && t.isSubmitted());
    }

    private static String html(String inner) {
        return "<html><div style='width:205px'>" + inner + "</div></html>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
