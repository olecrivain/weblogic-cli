package weblogiccli.cmd.exception;

/**
 * Utiliser pour signaler qu'un serveur n'est pas dispo.
 */
public class UnknownTargetException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnknownTargetException(String message) {
        super(message);
    }
}
