package weblogiccli.cmd.exception;

/**
 * Utiliser pour signaler une erreur dans les paramètres d'une commande.
 */
public class ArgsException extends Exception {

    private static final long serialVersionUID = 1L;

    public ArgsException() {
        super();
    }

    public ArgsException(String message) {
        super(message);
    }

    public ArgsException(String message, Throwable cause) {
        super(message, cause);
    }
}
