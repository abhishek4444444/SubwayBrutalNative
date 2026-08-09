package com.abhishek.subwaybrutal;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class FloatingMenuService extends Service {

    private static final String TAG = "SubwayBrutal_Float";
    private static final String TARGET_PKG = "com.kiloo.subwaysurf";

    private WindowManager windowManager;
    private View floatingIcon;
    private View panelView;
    private WindowManager.LayoutParams iconParams;
    private WindowManager.LayoutParams panelParams;

    private NativeBridge bridge;
    private Handler mainHandler;
    private TextView statusText;
    private boolean autoInjectDone = false;

    private final Map<String, Boolean> toggleStates = new LinkedHashMap<String, Boolean>();

    private static final String[][] FEATURES = {
        {"💰 Unlimited Coins",      NativeBridge.CMD_UNLIMITED_COINS},
        {"🏆 Auto Max Score",       NativeBridge.CMD_AUTO_SCORE},
        {"🛡️ God Mode (No Death)", NativeBridge.CMD_GOD_MODE},
        {"💥 No Obstacles",         NativeBridge.CMD_NO_OBSTACLES},
        {"🔧 Anti Crash",           NativeBridge.CMD_ANTI_CRASH},
        {"⚡ Speed Hack (3x)",      NativeBridge.CMD_SPEED_HACK},
        {"🦘 Super Jump",           NativeBridge.CMD_SUPER_JUMP},
        {"♾️ Infinite Jump",        NativeBridge.CMD_INFINITE_JUMP},
        {"✈️ Infinite Jetpack",     NativeBridge.CMD_INFINITE_JETPACK},
        {"🔓 Unlock All",           NativeBridge.CMD_UNLOCK_ALL},
        {"☢️ ACTIVATE ALL",         NativeBridge.CMD_ACTIVATE_ALL},
    };

    private static final int[] SECTION_BEFORE_INDEX = {0, 2, 5, 8, 9, 10};
    private static final String[] SECTION_NAMES = {
        "💰 COINS & SCORE", "🛡️ SURVIVAL", "🏃 MOVEMENT",
        "✈️ JETPACK", "🔓 UNLOCK", "☢️ NUCLEAR"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());

        for (String[] feat : FEATURES) toggleStates.put(feat[1], false);

        bridge = new NativeBridge();
        bridge.setCallback(new NativeBridge.ConnectionCallback() {
            @Override
            public void onConnected() {
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        if (statusText != null) {
                            statusText.setText("🟢 ATTACHED — Mod Active!");
                            statusText.setTextColor(Color.parseColor("#00FF88"));
                        }
                    }
                });
            }
            @Override
            public void onDisconnected() {
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        if (statusText != null) {
                            statusText.setText("🟡 Reconnecting…");
                            statusText.setTextColor(Color.parseColor("#FFD700"));
                        }
                    }
                });
                mainHandler.postDelayed(new Runnable() {
                    @Override public void run() { bridge.connect(); }
                }, 3000);
            }
            @Override
            public void onError(String msg) {
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        if (statusText != null) {
                            statusText.setText("⚪ Auto-injecting…");
                            statusText.setTextColor(Color.parseColor("#00FFFF"));
                        }
                    }
                });
                // Trigger auto-injection if not done
                if (!autoInjectDone) {
                    autoInjectDone = true;
                    performAutoInject();
                }
            }
        });

        createFloatingIcon();
        // Start auto-inject after 3 sec (give panel time to setup)
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() { performAutoInject(); }
        }, 3000);
        bridge.connect();
    }

    private void performAutoInject() {
        appendStatus("Detecting game…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Wait for game to be running
                    int retries = 0;
                    String gamePid = "";
                    while (retries < 30) {
                        gamePid = runShellOutput("pidof " + TARGET_PKG);
                        if (gamePid != null && !gamePid.trim().isEmpty()) break;
                        Thread.sleep(1000);
                        retries++;
                    }

                    if (gamePid == null || gamePid.trim().isEmpty()) {
                        appendStatus("Game not running yet");
                        return;
                    }

                    appendStatus("Game PID: " + gamePid.trim());

                    // Get .so path
                    String soPath = getApplicationInfo().nativeLibraryDir + "/libsubwaybrutal.so";
                    appendStatus("Copying .so…");

                    // Copy to /data/local/tmp
                    runShell("cp " + soPath + " /data/local/tmp/libsubwaybrutal.so");
                    runShell("chmod 777 /data/local/tmp/libsubwaybrutal.so");
                    runShell("setenforce 0");

                    // Kill and relaunch with LD_PRELOAD
                    appendStatus("Restarting game with mod…");
                    runShell("am force-stop " + TARGET_PKG);
                    Thread.sleep(2000);

                    // Launch with LD_PRELOAD via setprop trick
                    String launchCmd = "LD_PRELOAD=/data/local/tmp/libsubwaybrutal.so " +
                        "am start -n " + TARGET_PKG + "/com.unity3d.player.UnityPlayerActivity";
                    runShell(launchCmd);

                    appendStatus("Mod injected! Reconnecting…");
                    Thread.sleep(5000);

                    // Try to reconnect socket
                    if (bridge != null) bridge.connect();

                } catch (Exception e) {
                    Log.e(TAG, "Auto-inject err", e);
                    appendStatus("Inject error: " + e.getMessage());
                }
            }
        }, "AutoInject").start();
    }

    private void appendStatus(final String msg) {
        Log.d(TAG, "Status: " + msg);
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (statusText != null) {
                    statusText.setText("⚡ " + msg);
                    statusText.setTextColor(Color.parseColor("#00FFFF"));
                }
            }
        });
    }

    private void runShell(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Exception e) { Log.e(TAG, "Shell err: " + e.getMessage()); }
    }

    private String runShellOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) { return ""; }
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createFloatingIcon() {
        LinearLayout iconLayout = new LinearLayout(this);
        iconLayout.setGravity(Gravity.CENTER);
        iconLayout.setBackgroundColor(Color.parseColor("#FF0033"));

        TextView iconText = new TextView(this);
        iconText.setText("🔥");
        iconText.setTextSize(22f);
        iconText.setPadding(4, 4, 4, 4);
        iconLayout.addView(iconText);

        iconParams = new WindowManager.LayoutParams(
            dpToPx(52), dpToPx(52), getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = 20;
        iconParams.y = 200;

        final int[] iconInitialX = {0};
        final int[] iconInitialY = {0};
        final int[] touchInitialX = {0};
        final int[] touchInitialY = {0};
        final boolean[] moved = {false};
        final LinearLayout finalIconLayout = iconLayout;

        iconLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        iconInitialX[0] = iconParams.x;
                        iconInitialY[0] = iconParams.y;
                        touchInitialX[0] = (int) event.getRawX();
                        touchInitialY[0] = (int) event.getRawY();
                        moved[0] = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - touchInitialX[0];
                        int dy = (int) event.getRawY() - touchInitialY[0];
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved[0] = true;
                        iconParams.x = iconInitialX[0] + dx;
                        iconParams.y = iconInitialY[0] + dy;
                        windowManager.updateViewLayout(finalIconLayout, iconParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved[0]) togglePanel();
                        return true;
                }
                return false;
            }
        });

        floatingIcon = iconLayout;
        windowManager.addView(floatingIcon, iconParams);
    }

    private void togglePanel() {
        if (panelView != null && panelView.getVisibility() == View.VISIBLE)
            panelView.setVisibility(View.GONE);
        else if (panelView != null)
            panelView.setVisibility(View.VISIBLE);
        else
            createPanel();
    }

    private void createPanel() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int panelW = (int)(dm.widthPixels * 0.68f);
        int panelH = (int)(dm.heightPixels * 0.82f);

        panelParams = new WindowManager.LayoutParams(
            panelW, panelH, getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#E5080810"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#120012"));
        header.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(this);
        titleText.setText("🔥 SUBWAY BRUTAL v1.0");
        titleText.setTextColor(Color.parseColor("#FF0033"));
        titleText.setTextSize(13f);
        LinearLayout.LayoutParams titleLP = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleText.setLayoutParams(titleLP);

        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#FFD700"));
        closeBtn.setTextSize(16f);
        closeBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { panelView.setVisibility(View.GONE); }
        });

        header.addView(titleText);
        header.addView(closeBtn);

        final int[] panelInitX = {0};
        final int[] panelInitY = {0};
        final int[] touchInitX = {0};
        final int[] touchInitY = {0};
        header.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        panelInitX[0] = panelParams.x;
                        panelInitY[0] = panelParams.y;
                        touchInitX[0] = (int) event.getRawX();
                        touchInitY[0] = (int) event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        panelParams.x = panelInitX[0] + (int)event.getRawX() - touchInitX[0];
                        panelParams.y = panelInitY[0] + (int)event.getRawY() - touchInitY[0];
                        windowManager.updateViewLayout(panelView, panelParams);
                        return true;
                }
                return false;
            }
        });

        statusText = new TextView(this);
        statusText.setText("⚡ Auto-injecting on start…");
        statusText.setTextColor(Color.parseColor("#00FFFF"));
        statusText.setTextSize(10f);
        statusText.setBackgroundColor(Color.parseColor("#0D0D1A"));
        statusText.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setBackgroundColor(Color.parseColor("#08080F"));
        LinearLayout.LayoutParams scrollLP = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0);
        scrollLP.weight = 1f;
        scrollView.setLayoutParams(scrollLP);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(30));
        scrollContent.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int sectionIdx = 0;
        for (int i = 0; i < FEATURES.length; i++) {
            if (sectionIdx < SECTION_BEFORE_INDEX.length && i == SECTION_BEFORE_INDEX[sectionIdx]) {
                scrollContent.addView(makeSectionHeader(SECTION_NAMES[sectionIdx]));
                sectionIdx++;
            }
            scrollContent.addView(makeToggleRow(FEATURES[i][0], FEATURES[i][1]));
        }

        scrollView.addView(scrollContent);
        rootLayout.addView(header);
        rootLayout.addView(statusText);
        rootLayout.addView(scrollView);

        panelView = rootLayout;
        windowManager.addView(panelView, panelParams);
    }

    private View makeSectionHeader(String title) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#1A001A"));
        row.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(6), 0, dpToPx(2));
        row.setLayoutParams(lp);
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#FFD700"));
        tv.setTextSize(11f);
        row.addView(tv);
        return row;
    }

    private View makeToggleRow(final String label, final String command) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.parseColor("#0F0F1F"));
        row.setPadding(dpToPx(10), dpToPx(9), dpToPx(10), dpToPx(9));
        LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLP.setMargins(0, dpToPx(1), 0, dpToPx(1));
        row.setLayoutParams(rowLP);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(12f);
        LinearLayout.LayoutParams labelLP = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelLP);

        final TextView toggleView = new TextView(this);
        boolean cs = Boolean.TRUE.equals(toggleStates.get(command));
        toggleView.setText(cs ? "ON" : "OFF");
        toggleView.setTextColor(cs ? Color.parseColor("#00FF88") : Color.parseColor("#444444"));
        toggleView.setTextSize(11f);
        toggleView.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = !Boolean.TRUE.equals(toggleStates.get(command));
                toggleStates.put(command, newState);
                toggleView.setText(newState ? "ON" : "OFF");
                toggleView.setTextColor(newState ? Color.parseColor("#00FF88") : Color.parseColor("#444444"));
                row.setBackgroundColor(newState ? Color.parseColor("#0A1F0A") : Color.parseColor("#0F0F1F"));
                if (command.equals(NativeBridge.CMD_ACTIVATE_ALL)) activateAll(newState);
                else bridge.sendToggle(command, newState);
            }
        });

        row.addView(labelView);
        row.addView(toggleView);
        return row;
    }

    private void activateAll(boolean enable) {
        for (String[] feat : FEATURES) {
            String cmd = feat[1];
            if (!cmd.equals(NativeBridge.CMD_ACTIVATE_ALL)) {
                toggleStates.put(cmd, enable);
                bridge.sendToggle(cmd, enable);
            }
        }
        if (panelView != null) {
            try { windowManager.removeView(panelView); } catch (Exception e) {}
            panelView = null;
        }
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() { createPanel(); }
        }, 200);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingIcon != null)
            try { windowManager.removeView(floatingIcon); } catch (Exception e) {}
        if (panelView != null)
            try { windowManager.removeView(panelView); } catch (Exception e) {}
        if (bridge != null) bridge.disconnect();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
