package weblogiccli.logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PatternLayoutEncoderFixedForWindows extends PatternLayoutEncoder {

    public static String encoding;
    static {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // Encodage des accents dans la console windows
            encoding = "Cp850";
        }
    }
    
    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        String txt = layout.doLayout(event);
        outputStream.write(convertToBytes(txt));
        outputStream.flush();
    }

    private byte[] convertToBytes(String s) {
        if (encoding == null) {
            return s.getBytes();
        } else {
            try {
                return s.getBytes(encoding);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("An existing charset cannot possibly be unsupported.");
            }
        }
    }
}