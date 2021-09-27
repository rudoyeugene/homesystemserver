package com.rudyii.hsw.configuration;

import com.rudyii.hsw.objects.events.SettingsUpdatedEvent;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.PrintStream;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

@Slf4j
@Component
@RequiredArgsConstructor
public class Logger {
    private final FirebaseGlobalSettingsService globalSettingsService;
    private PrintStream defaultOutPrintStream = System.out;
    private PrintStream defaultErrPrintStream = System.err;

    @PostConstruct
    public void tieSystemOutAndErrToLog() {
        if (globalSettingsService.getGlobalSettings().isVerboseOutput()) {
            System.setOut(createLoggingProxy(System.out, INFO));
            System.setErr(createLoggingProxy(System.err, ERROR));
        }
    }

    @EventListener(SettingsUpdatedEvent.class)
    public void switchOutput(SettingsUpdatedEvent event) {
        if (event.getGlobalSettings().isVerboseOutput()) {
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
