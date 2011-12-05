package weblogiccli.cmd.exception;

/**
 * Utiliser pour signaler qu'un serveur n'est pas dispo.
 */
public class ServerIsNotRunningException extends Exception {

    private static final long serialVersionUID = 1L;

    public ServerIsNotRunningException(String message) {
        super(message);
    }
}
