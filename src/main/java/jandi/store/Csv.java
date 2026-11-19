package jandi.store;

import java.util.ArrayList;
import java.util.List;

/**
 * 아주 작은 CSV 읽기·쓰기 도구. 쉼표, 따옴표, 줄바꿈이 든 값은 따옴표로 감싼다.
 *
 * @author Team 3
 * @version 0.1
 */
public final class Csv {

    private Csv() {
    }

    /**
     * 값들을 CSV 한 줄로 만든다.
     *
     * @param fields 값들
     * @return 줄바꿈이 붙지 않은 CSV 한 줄
     */
    public static String line(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String f = fields[i] == null ? "" : fields[i];
            if (f.contains(",") || f.contains("\"") || f.contains("\n") || f.contains("\r")) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }

    /**
     * CSV 글 전체를 줄과 값으로 나눈다. 빈 줄은 건너뛴다.
     *
     * @param text CSV 글
     * @return 줄마다의 값 배열
     */
    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
                any = true;
            } else if (ch == ',') {
                row.add(cur.toString());
                cur.setLength(0);
                any = true;
            } else if (ch == '\n' || ch == '\r') {
                if (any || cur.length() > 0) {
                    row.add(cur.toString());
                    rows.add(row.toArray(new String[0]));
                }
                row = new ArrayList<>();
                cur.setLength(0);
                any = false;
            } else {
                cur.append(ch);
            }
        }
        if (any || cur.length() > 0) {
            row.add(cur.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }
}
