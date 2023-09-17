package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger implements ILogger {
    private final String logFileName;
    private final SimpleDateFormat dateFormat;

    public Logger(String logFileName) {
        this.logFileName = logFileName;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public void logMessage(String message) {
        if (!message.contains("/exit")) {
            try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFileName, true))) {
                logWriter.println("[" + dateFormat.format(new Date()) + "] " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
