package weblogiccli.logger;

import weblogiccli.utils.Colorize;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ColoredException extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return Colorize.red(super.convert(event));
        } else if (event.getLevel().equals(Level.WARN)) {
            return Colorize.yellow(super.convert(event));
        } else {
            return super.convert(event);
        }
    }
}