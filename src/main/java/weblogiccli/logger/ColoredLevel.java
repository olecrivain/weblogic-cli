package weblogiccli.logger;

import weblogiccli.utils.Colorize;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ColoredLevel extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String level = "~";
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return Colorize.red(level);
        } else if (event.getLevel().equals(Level.WARN)) {
            return Colorize.yellow(level);
        } else {
            return level;
        }
    }
}