package com.rudyii.hsw.configuration;

import com.rudyii.hsw.springcore.HomeSystem;
import org.apache.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.PrintStream;

import static org.apache.log4j.Level.ERROR;
import static org.apache.log4j.Level.INFO;

@Component
public class OutputSwitcher {
    private static Logger LOG = LogManager.getLogger(HomeSystem.class);

    @PostConstruct
    public void tieSystemOutAndErrToLog() {
        System.setOut(createLoggingProxy(System.out, INFO));
        System.setErr(createLoggingProxy(System.err, ERROR));
    }

    private PrintStream createLoggingProxy(final PrintStream realPrintStream, Level level) {
        if (level.equals(ERROR)) {
            return new PrintStream(realPrintStream) {
                public void print(final String string) {
                    realPrintStream.print(string);
                    LOG.error(string);
                }
            };
        } else {
            return new PrintStream(realPrintStream) {
                public void print(final String string) {
                    realPrintStream.print(string);
                    LOG.info(string);
                }
            };
        }
    }
}
