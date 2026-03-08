package com.vkmu.datadestination.connection;

import com.vkmu.datadestination.debug.DebugLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class DesktopReceiver implements Runnable {

    private static final int PORT = 9000;
    private boolean running = true;

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (running) {
                Socket socket = serverSocket.accept();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                String line = reader.readLine();
                if (line != null) {
                    DebugLogger.log("Desktop: " + line);
                }

                socket.close();
            }

        } catch (Exception e) {
            DebugLogger.log("Server error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
