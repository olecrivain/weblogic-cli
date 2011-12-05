package weblogiccli.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Console {

    private static Logger LOG = LoggerFactory.getLogger(Console.class);

	public abstract void run(String[] args) throws Exception;
    
    public static void printHeading(String msg) {
        printHorizontalRule();
        LOG.info(msg);
        printHorizontalRule();
    }
    
    public static void printHorizontalRule() {
        LOG.info("------------------------------------------------------------------------");
    }
    
    public static void printFailure() {
        printHeading(Colorize.red("ÉCHEC"));
    }
    
    public static void printSuccess() {
        printHeading(Colorize.green("SUCCÈS"));
    }
    
    public static void printEmptyLine() {
        LOG.info("");
    }
}
