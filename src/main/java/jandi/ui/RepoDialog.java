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
 * @author Team 3
 * @version 0.1
 */
public class RepoDialog extends JDialog {

    private final Ranch ranch;
    private final JTextField path = new JTextField(30);
    private final Map<Member, JComboBox<String>> combos = new LinkedHashMap<>();

    /**
     * @param owner 부모 창
     * @param ranch 목장
     */
    public RepoDialog(Frame owner, Ranch ranch) {
        super(owner, "git 저장소 연결", true);
        this.ranch = ranch;
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
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

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setBorder(BorderFactory.createTitledBorder("팀원 ↔ git 작성자 (이름 또는 이메일)"));
        for (Member m : ranch.members()) {
            JComboBox<String> box = new JComboBox<>();
            box.setEditable(true);
            combos.put(m, box);
            grid.add(new JLabel(m.getName()));
            grid.add(box);
        }
        fillAuthors();
        root.add(grid, BorderLayout.CENTER);

        UiKit.RoundButton save = new UiKit.RoundButton("저장", true);
        UiKit.RoundButton close = new UiKit.RoundButton("닫기", false);
        save.addActionListener(e -> {
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

    private void browse() {
        JFileChooser fc = new JFileChooser(ranch.repoPath() == null ? null : ranch.repoPath().toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("git 폴더 고르기");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path p = fc.getSelectedFile().toPath();
        try {
            ranch.connectRepo(p);
            path.setText(p.toString());
            fillAuthors();
        } catch (JandiException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "잔디", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void fillAuthors() {
        List<String> authors = GitService.authors(ranch.commits());
        combos.forEach((m, box) -> {
            box.removeAllItems();
            authors.forEach(box::addItem);
            box.getEditor().setItem(m.getGitAuthor());
        });
    }
}
