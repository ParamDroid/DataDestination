package com.vkmu.datadestination.debug;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.LinkedList;

public class DebugLogger {

    private static TextView debugView;
    private static final int MAX_LINES = 40;
    private static final LinkedList<String> lines = new LinkedList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static boolean isDebugEnabled(Context context) {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("debug_mode", true);
    }
    private DebugLogger() {}

    public static void attach(TextView view) {
        debugView = view;
    }

    public static void log(String message) {

        if (message == null) return;

        // ✅ FIRST: check if debugView exists
        if (debugView == null) return;

        // ✅ NOW safe to use context
        if (!isDebugEnabled(debugView.getContext())) return;

        synchronized (lines) {
            lines.add(message);

            if (lines.size() > MAX_LINES) {
                lines.removeFirst();
            }
        }

        mainHandler.post(() -> {
            StringBuilder builder = new StringBuilder();

            synchronized (lines) {
                for (String line : lines) {
                    builder.append(line).append("\n");
                }
            }

            debugView.setText(builder.toString());
        });
    }
}