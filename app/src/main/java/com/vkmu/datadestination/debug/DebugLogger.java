package com.vkmu.datadestination.debug;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.LinkedList;

public class DebugLogger {

    private static TextView debugView;
    private static final int MAX_LINES = 40;
    private static final LinkedList<String> lines = new LinkedList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DebugLogger() {}

    public static void attach(TextView view) {
        debugView = view;
    }

    public static void log(String message) {

        if (message == null) return;

        synchronized (lines) {
            lines.add(message);

            if (lines.size() > MAX_LINES) {
                lines.removeFirst();
            }
        }

        if (debugView == null) return;

        mainHandler.post(() -> {
            StringBuilder builder = new StringBuilder();
            for (String line : lines) {
                builder.append(line).append("\n");
            }
            debugView.setText(builder.toString());
        });
    }
}