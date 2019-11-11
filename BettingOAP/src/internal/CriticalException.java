package internal;

public class CriticalException extends RuntimeException {
    public CriticalException() {
        super();
    }

    public CriticalException(Exception e) {
        super(e);
    }

    public CriticalException(String message) {
        super(message);
    }
}
