package jandi.ui;

import jandi.domain.Member;
import jandi.service.GitService;
import jandi.service.JandiException;
import jandi.service.Ranch;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * git 저장소 연결 창. 로컬 git 폴더를 고르고, 팀원마다 git 작성자를 짝짓는다.
 *
 * <p>폴더는 고르는 즉시 연결되고, 팀원과 작성자의 짝은 "저장"을 눌러야 반영된다.
 * 모달 대화상자라서, 이 창이 닫힌 뒤에 {@link MainFrame}이 화면을 다시 그린다.</p>
 *
 * @author Team 3
 * @version 0.1
 */
public class RepoDialog extends JDialog {

    private final Ranch ranch;
    /** 연결된 폴더 경로를 보여 주는 칸. 직접 고칠 수 없고 "폴더 고르기"로만 바뀐다. */
    private final JTextField path = new JTextField(30);
    /** 팀원마다 git 작성자를 고르는 콤보 상자. 넣은 순서(팀원 순서)를 지키려고 LinkedHashMap을 쓴다. */
    private final Map<Member, JComboBox<String>> combos = new LinkedHashMap<>();

    /**
     * 위에 폴더 경로, 가운데에 팀원별 작성자 고르기, 아래에 닫기와 저장 버튼을 배치한다.
     *
     * @param owner 부모 창
     * @param ranch 목장
     */
    public RepoDialog(Frame owner, Ranch ranch) {
        super(owner, "git 저장소 연결", true);
        this.ranch = ranch;
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        // 위: 안내 글 + 경로 칸 + "폴더 고르기" 버튼
        path.setEditable(false);
        path.setText(ranch.repoPath() == null ? "" : ranch.repoPath().toString());
        UiKit.RoundButton browse = new UiKit.RoundButton("폴더 고르기", false);
        browse.addActionListener(e -> browse());
        JPanel top = new JPanel(new BorderLayout(8, 4));
        JLabel l = new JLabel("로컬 git 폴더 (팀이 함께 쓰는 저장소 하나)");
        l.setFont(UiKit.font(Font.BOLD, 13));
        top.add(l, BorderLayout.NORTH);
        top.add(path, BorderLayout.CENTER);
        top.add(browse, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        // 가운데: 팀원 이름과 콤보 상자를 2열 격자로 놓는다. GridLayout(0, 2)는 열이 2개이고 행은 넣는 만큼 늘어난다.
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setBorder(BorderFactory.createTitledBorder("팀원 ↔ git 작성자 (이름 또는 이메일)"));
        for (Member m : ranch.members()) {
            JComboBox<String> box = new JComboBox<>();
            box.setEditable(true); // 목록에서 고를 수도 있고, 이름이나 이메일을 직접 적을 수도 있다.
            combos.put(m, box);
            grid.add(new JLabel(m.getName()));
            grid.add(box);
        }
        fillAuthors();
        root.add(grid, BorderLayout.CENTER);

        // 아래: 닫기와 저장 버튼(오른쪽 정렬)
        UiKit.RoundButton save = new UiKit.RoundButton("저장", true);
        UiKit.RoundButton close = new UiKit.RoundButton("닫기", false);
        save.addActionListener(e -> {
            // 콤보 상자에 지금 적혀 있는 글(목록에 없는, 직접 적은 값 포함)을 그 팀원의 git 작성자로 저장한다.
            combos.forEach((m, box) -> ranch.setGitAuthor(m.getId(), String.valueOf(box.getEditor().getItem())));
            dispose();
        });
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.add(close);
        bottom.add(save);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    /** 폴더 고르기 창을 띄우고, 고른 폴더를 git 저장소로 연결한다. git 저장소가 아니면 경고만 하고 그대로 둔다. */
    private void browse() {
        JFileChooser fc = new JFileChooser(ranch.repoPath() == null ? null : ranch.repoPath().toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // 파일이 아니라 폴더만 고르게 한다.
        fc.setDialogTitle("git 폴더 고르기");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) { // 취소했으면 아무것도 하지 않는다.
            return;
        }
        Path p = fc.getSelectedFile().toPath();
        try {
            ranch.connectRepo(p); // git 저장소가 아니면 JandiException을 던진다.
            path.setText(p.toString());
            fillAuthors(); // 새 저장소의 작성자들로 콤보 상자를 다시 채운다.
        } catch (JandiException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "잔디", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** 콤보 상자마다 커밋에서 찾은 작성자 목록을 넣고, 이미 짝지어 둔 작성자를 적어 둔다. */
    private void fillAuthors() {
        List<String> authors = GitService.authors(ranch.commits());
        combos.forEach((m, box) -> {
            box.removeAllItems();
            authors.forEach(box::addItem);
            box.getEditor().setItem(m.getGitAuthor());
        });
    }
}
