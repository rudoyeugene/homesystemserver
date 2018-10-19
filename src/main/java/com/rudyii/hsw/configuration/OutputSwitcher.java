package com.rudyii.hsw.configuration;

import com.rudyii.hsw.objects.events.OptionsChangedEvent;
import com.rudyii.hsw.springcore.HomeSystem;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.PrintStream;

import static com.rudyii.hsw.configuration.OptionsService.VERBOSE_OUTPUT_ENABLED;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

@Component
public class OutputSwitcher {
    private static Logger LOG = LogManager.getLogger(HomeSystem.class);
    private PrintStream defaultOutPrintStream = System.out;
    private PrintStream defaultErrPrintStream = System.err;
    private OptionsService optionsService;

    @Autowired
    public OutputSwitcher(OptionsService optionsService) {
        this.optionsService = optionsService;
    }

    @PostConstruct
    public void tieSystemOutAndErrToLog() {
        if ((boolean) optionsService.getOption(VERBOSE_OUTPUT_ENABLED)) {
            System.setOut(createLoggingProxy(System.out, INFO));
            System.setErr(createLoggingProxy(System.err, ERROR));
        }
    }

    @EventListener(OptionsChangedEvent.class)
    public void switchOutput(OptionsChangedEvent event) {
        if ((boolean) event.getOption(VERBOSE_OUTPUT_ENABLED)) {
            System.setOut(createLoggingProxy(System.out, INFO));
            System.setErr(createLoggingProxy(System.err, ERROR));
        } else {
            System.setOut(defaultOutPrintStream);
            System.setErr(defaultErrPrintStream);
        }
    }

    private PrintStream createLoggingProxy(final PrintStream realPrintStream, Level level) {
        if (level.equals(ERROR)) {
            return new PrintStream(realPrintStream) {
                public void print(final String string) {
                    LOG.error(string);
                }
            };
        } else {
            return new PrintStream(realPrintStream) {
                public void print(final String string) {
                    LOG.info(string);
                }
            };
        }
    }
}
