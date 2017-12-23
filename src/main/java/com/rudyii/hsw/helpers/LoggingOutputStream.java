package com.rudyii.hsw.helpers;

import java.io.IOException;
import java.io.PrintStream;

//Copy-Paste...

public class LoggingOutputStream extends PrintStream {
    public LoggingOutputStream(String fileName) throws IOException {
        super(fileName);
    }

    @Override
    public void println(String line) {
        super.println("[DEBUG] " + Thread.currentThread().getName() + ": " + line);
    }
}
