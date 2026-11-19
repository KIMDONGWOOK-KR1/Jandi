package jandi.service;

/**
 * 잔디에서 사용자에게 알려야 하는 문제. 메시지를 그대로 화면에 보여 준다.
 *
 * @author Team 3
 * @version 0.1
 */
public class JandiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message 사용자에게 보여 줄 안내 */
    public JandiException(String message) {
        super(message);
    }
}
