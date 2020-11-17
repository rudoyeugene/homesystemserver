package com.rudyii.hsw.configuration;

import com.rudyii.hsw.objects.events.OptionsChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.PrintStream;

import static com.rudyii.hsw.configuration.OptionsService.VERBOSE_OUTPUT_ENABLED;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

@Slf4j
@Component
public class Logger {
    private final PrintStream defaultOutPrintStream = System.out;
    private final PrintStream defaultErrPrintStream = System.err;
    private final OptionsService optionsService;

    @Autowired
    public Logger(OptionsService optionsService) {
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
                    log.error(string);
                }
            };
        } else {
            return new PrintStream(realPrintStream) {
                public void print(final String string) {
                    log.info(string);
                }
            };
        }
    }
}
