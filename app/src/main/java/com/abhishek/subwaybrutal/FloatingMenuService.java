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

import java.util.LinkedHashMap;
import java.util.Map;

public class FloatingMenuService extends Service {

    private static final String TAG = "SubwayBrutal_Float";

    private WindowManager windowManager;
    private View floatingIcon;
    private View panelView;
    private WindowManager.LayoutParams iconParams;
    private WindowManager.LayoutParams panelParams;

    private NativeBridge bridge;
    private Handler mainHandler;
    private TextView statusText;

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
        "💰 COINS & SCORE",
        "🛡️ SURVIVAL",
        "🏃 MOVEMENT",
        "✈️ JETPACK",
        "🔓 UNLOCK",
        "☢️ NUCLEAR"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());

        for (String[] feat : FEATURES) {
            toggleStates.put(feat[1], false);
        }

        bridge = new NativeBridge();
        bridge.setCallback(new NativeBridge.ConnectionCallback() {
            @Override
            public void onConnected() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (statusText != null) {
                            statusText.setText("🟢 ATTACHED — Mod Active");
                            statusText.setTextColor(Color.parseColor("#00FF88"));
                        }
                    }
                });
            }
            @Override
            public void onDisconnected() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (statusText != null) {
                            statusText.setText("🔴 DISCONNECTED");
                            statusText.setTextColor(Color.parseColor("#FF3333"));
                        }
                    }
                });
            }
            @Override
            public void onError(String msg) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (statusText != null) {
                            statusText.setText("⚪ WAITING FOR GAME…");
                            statusText.setTextColor(Color.parseColor("#888888"));
                        }
                    }
                });
            }
        });

        createFloatingIcon();
        bridge.connect();
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
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
            dpToPx(52),
            dpToPx(52),
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
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
        if (panelView != null && panelView.getVisibility() == View.VISIBLE) {
            panelView.setVisibility(View.GONE);
        } else if (panelView != null) {
            panelView.setVisibility(View.VISIBLE);
        } else {
            createPanel();
        }
    }

    private void createPanel() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;
        int panelW = (int)(screenW * 0.68f);
        int panelH = (int)(screenH * 0.82f);

        panelParams = new WindowManager.LayoutParams(
            panelW, panelH,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.CENTER;

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#E5080810"));

        // HEADER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#120012"));
        header.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(this);
        titleText.setText("🔥 SUBWAY BRUTAL v1.0");
        titleText.setTextColor(Color.parseColor("#FF0033"));
        titleText.setTextSize(13f);
        LinearLayout.LayoutParams titleLP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleText.setLayoutParams(titleLP);

        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#FFD700"));
        closeBtn.setTextSize(16f);
        closeBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panelView.setVisibility(View.GONE);
            }
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

        // STATUS
        statusText = new TextView(this);
        statusText.setText("⚪ WAITING FOR GAME…");
        statusText.setTextColor(Color.parseColor("#888888"));
        statusText.setTextSize(10f);
        statusText.setBackgroundColor(Color.parseColor("#0D0D1A"));
        statusText.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));

        // SCROLL VIEW - FIXED
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
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

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
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
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
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLP.setMargins(0, dpToPx(1), 0, dpToPx(1));
        row.setLayoutParams(rowLP);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(12f);
        LinearLayout.LayoutParams labelLP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelLP);

        final TextView toggleView = new TextView(this);
        boolean currentState = Boolean.TRUE.equals(toggleStates.get(command));
        toggleView.setText(currentState ? "ON" : "OFF");
        toggleView.setTextColor(currentState ? Color.parseColor("#00FF88") : Color.parseColor("#444444"));
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

                if (command.equals(NativeBridge.CMD_ACTIVATE_ALL)) {
                    activateAll(newState);
                } else {
                    bridge.sendToggle(command, newState);
                }
                Log.d(TAG, "Toggle: " + command + " -> " + newState);
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
            @Override
            public void run() {
                createPanel();
            }
        }, 200);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingIcon != null) {
            try { windowManager.removeView(floatingIcon); } catch (Exception e) {}
        }
        if (panelView != null) {
            try { windowManager.removeView(panelView); } catch (Exception e) {}
        }
        if (bridge != null) bridge.disconnect();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
