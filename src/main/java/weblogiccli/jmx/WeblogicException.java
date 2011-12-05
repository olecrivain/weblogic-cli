package weblogiccli.jmx;

public class WeblogicException extends Exception {

    private static final long serialVersionUID = 1L;

    public WeblogicException(String message) {
        super(message);
    }

    public WeblogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
