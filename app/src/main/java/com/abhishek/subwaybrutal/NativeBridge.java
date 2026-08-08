package com.abhishek.subwaybrutal;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ConnectException;

public class NativeBridge {

    private static final String TAG = "SubwayBrutal_Bridge";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 19877;
    private static final int TIMEOUT_MS = 3000;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean connected = false;

    public static final String CMD_UNLIMITED_COINS    = "UNLIMITED_COINS";
    public static final String CMD_GOD_MODE           = "GOD_MODE";
    public static final String CMD_INFINITE_JUMP      = "INFINITE_JUMP";
    public static final String CMD_SPEED_HACK         = "SPEED_HACK";
    public static final String CMD_SUPER_JUMP         = "SUPER_JUMP";
    public static final String CMD_INFINITE_JETPACK   = "INFINITE_JETPACK";
    public static final String CMD_NO_OBSTACLES       = "NO_OBSTACLES";
    public static final String CMD_AUTO_SCORE         = "AUTO_SCORE";
    public static final String CMD_UNLOCK_ALL         = "UNLOCK_ALL";
    public static final String CMD_ANTI_CRASH         = "ANTI_CRASH";
    public static final String CMD_ACTIVATE_ALL       = "ACTIVATE_ALL";

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onError(String msg);
    }

    private ConnectionCallback callback;

    public void setCallback(ConnectionCallback cb) {
        this.callback = cb;
    }

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retries = 0;
                while (retries < 10 && !connected) {
                    try {
                        Thread.sleep(1500);
                        socket = new Socket();
                        socket.connect(new java.net.InetSocketAddress(HOST, PORT), TIMEOUT_MS);
                        socket.setSoTimeout(TIMEOUT_MS);
                        writer = new PrintWriter(socket.getOutputStream(), true);
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        connected = true;
                        Log.d(TAG, "Connected");
                        if (callback != null) callback.onConnected();
                        startReadLoop();
                        break;
                    } catch (ConnectException e) {
                        retries++;
                        Log.w(TAG, "Retry " + retries + ": " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                        if (callback != null) callback.onError(e.getMessage());
                        break;
                    }
                }
                if (!connected && callback != null) {
                    callback.onError("Cannot connect after 10 retries");
                }
            }
        }, "BridgeConnect").start();
    }

    private void startReadLoop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while (connected && (line = reader.readLine()) != null) {
                        Log.d(TAG, "Native: " + line);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Read ended: " + e.getMessage());
                } finally {
                    connected = false;
                    if (callback != null) callback.onDisconnected();
                }
            }
        }, "BridgeRead").start();
    }

    public void sendToggle(final String command, final boolean enabled) {
        if (!connected || writer == null) {
            Log.w(TAG, "Not connected: " + command);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg = command + ":" + (enabled ? "1" : "0");
                    writer.println(msg);
                    Log.d(TAG, "Sent: " + msg);
                } catch (Exception e) {
                    Log.e(TAG, "Send err: " + e.getMessage());
                    connected = false;
                }
            }
        }, "BridgeSend").start();
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Disconnect err: " + e.getMessage());
        }
    }
}
