package weblogiccli.cmd.exception;

/**
 * Utiliser pour signaler un environnement inconnu.
 */
public class EnvironmentException extends Exception {

    private static final long serialVersionUID = 1L;

    public EnvironmentException() {
        super();
    }

    public EnvironmentException(String message) {
        super(message);
    }
}
