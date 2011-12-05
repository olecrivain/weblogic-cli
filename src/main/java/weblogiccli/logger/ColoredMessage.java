package weblogiccli.logger;

import weblogiccli.utils.Colorize;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ColoredMessage extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return Colorize.red(message);
        } else if (event.getLevel().equals(Level.WARN)) {
            return Colorize.yellow(message);
        } else {
            return message;
        }
    }
}