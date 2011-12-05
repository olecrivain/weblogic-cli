package weblogiccli.utils;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;

import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;

public class Colorize {

    static {
        AnsiConsole.systemInstall();
    }

    public static String red(Object o) {
        return colorize(o, RED);
    }

    public static String green(Object o) {
        return colorize(o, GREEN);
    }

    public static String yellow(Object o) {
        return colorize(o, YELLOW);
    }

    private static String colorize(Object o, Color color) {
        return ansi().fg(color).a(o.toString()).reset().toString();
    }
}