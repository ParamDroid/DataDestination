package com.vkmu.datadestination.debug;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DebugLogger {

    private static final int MAX_LINES = 40;
    private static final int UPDATE_DELAY_MS = 250;

    private static final LinkedList<String> lines =
            new LinkedList<>();

    private static final List<LogListener> listeners =
            new ArrayList<>();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private static boolean updateScheduled = false;

    private static volatile boolean debugEnabled = true;

    public interface LogListener {
        void onLogsChanged(String logs);
    }

    private DebugLogger() {}

    public static void observe(LifecycleOwner owner, LogListener listener) {
        if (owner == null || listener == null) return;

        Lifecycle lifecycle = owner.getLifecycle();

        LifecycleEventObserver observer = new LifecycleEventObserver() {
            private boolean registered = false;

            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    addListener(listener);
                    registered = true;
                } else if (event == Lifecycle.Event.ON_STOP) {
                    removeListener(listener);
                    registered = false;
                } else if (event == Lifecycle.Event.ON_DESTROY) {
                    if (registered) {
                        removeListener(listener);
                    }
                    source.getLifecycle().removeObserver(this);
                }
            }
        };

        lifecycle.addObserver(observer);

        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            addListener(listener);
        }
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void log(String message) {

        if (!debugEnabled) return;

        if (message == null) return;

        synchronized (lines) {

            lines.add(message);

            while (lines.size() > MAX_LINES) {
                lines.removeFirst();
            }
        }

        scheduleUiUpdate();
    }

    private static void addListener(LogListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        dispatchCurrentLogs(listener);
    }

    private static void removeListener(LogListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private static void scheduleUiUpdate() {

        synchronized (DebugLogger.class) {

            if (updateScheduled) return;

            updateScheduled = true;
        }

        mainHandler.postDelayed(() -> {

            synchronized (DebugLogger.class) {
                updateScheduled = false;
            }

            String logs = buildLogText();
            List<LogListener> currentListeners = getListenersSnapshot();

            for (LogListener listener : currentListeners) {
                listener.onLogsChanged(logs);
            }

        }, UPDATE_DELAY_MS);
    }

    private static void dispatchCurrentLogs(LogListener listener) {
        String logs = buildLogText();

        mainHandler.post(() -> {
            synchronized (listeners) {
                if (!listeners.contains(listener)) return;
            }

            listener.onLogsChanged(logs);
        });
    }

    private static String buildLogText() {
        StringBuilder builder =
                new StringBuilder();

        synchronized (lines) {

            for (String line : lines) {

                builder.append(line)
                        .append("\n");
            }
        }

        return builder.toString();
    }

    private static List<LogListener> getListenersSnapshot() {
        synchronized (listeners) {
            return new ArrayList<>(listeners);
        }
    }
}
