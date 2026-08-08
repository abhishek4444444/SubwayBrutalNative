package com.abhishek.subwaybrutal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private static final String TAG = "SubwayBrutal_Main";
    private static final String TARGET_PKG = "com.kiloo.subwaysurf";
    private static final int OVERLAY_REQUEST_CODE = 1001;

    private TextView statusText;
    private TextView logText;
    private Handler handler;
    private boolean injected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        buildUI();
        checkRoot();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0A0A0A"));
        root.setPadding(24, 40, 24, 24);
        root.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("🔥 SUBWAY BRUTAL v1.0");
        title.setTextColor(Color.parseColor("#FF0033"));
        title.setTextSize(22f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 4);

        TextView sub = new TextView(this);
        sub.setText("by Abhishek | Target: com.kiloo.subwaysurf");
        sub.setTextColor(Color.parseColor("#888888"));
        sub.setTextSize(11f);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 24);

        statusText = new TextView(this);
        statusText.setText("⚪ CHECKING ROOT…");
        statusText.setTextColor(Color.parseColor("#FFD700"));
        statusText.setTextSize(13f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 20);

        Button launchBtn = new Button(this);
        launchBtn.setText("▶ LAUNCH SUBWAY SURFERS");
        launchBtn.setTextColor(Color.WHITE);
        launchBtn.setTextSize(14f);
        launchBtn.setBackgroundColor(Color.parseColor("#FF0033"));
        launchBtn.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams btnLP = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLP.setMargins(0, 0, 0, 12);
        launchBtn.setLayoutParams(btnLP);
        launchBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                launchAndInject();
            }
        });

        Button injectBtn = new Button(this);
        injectBtn.setText("⚡ INJECT MOD (Game Running)");
        injectBtn.setTextColor(Color.WHITE);
        injectBtn.setTextSize(13f);
        injectBtn.setBackgroundColor(Color.parseColor("#1A0033"));
        injectBtn.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams injLP = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        injLP.setMargins(0, 0, 0, 12);
        injectBtn.setLayoutParams(injLP);
        injectBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                injectOnly();
            }
        });

        logText = new TextView(this);
        logText.setText("Logs will appear here…\n");
        logText.setTextColor(Color.parseColor("#00FFFF"));
        logText.setTextSize(10f);
        logText.setBackgroundColor(Color.parseColor("#050510"));
        logText.setPadding(12, 8, 12, 8);
        LinearLayout.LayoutParams logLP = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0);
        logLP.weight = 1f;
        logText.setLayoutParams(logLP);

        TextView wm = new TextView(this);
        wm.setText("💀 SUBWAY BRUTAL — USE WISELY");
        wm.setTextColor(Color.parseColor("#333333"));
        wm.setTextSize(9f);
        wm.setGravity(Gravity.CENTER);
        wm.setPadding(0, 12, 0, 0);

        root.addView(title);
        root.addView(sub);
        root.addView(statusText);
        root.addView(launchBtn);
        root.addView(injectBtn);
        root.addView(logText);
        root.addView(wm);
        setContentView(root);
    }

    private void checkRoot() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean rooted = false;
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String out = br.readLine();
                    p.waitFor();
                    rooted = (out != null && out.contains("uid=0"));
                } catch (Exception e) {
                    rooted = false;
                }
                final boolean finalRooted = rooted;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalRooted) {
                            setStatus("🟢 ROOT DETECTED — Ready!", "#00FF88");
                            appendLog("Root: PASSED");
                            checkOverlayPermission();
                        } else {
                            setStatus("🔴 NO ROOT — App needs root!", "#FF3333");
                            appendLog("Root: FAILED");
                            Toast.makeText(MainActivity.this, "ROOT NOT FOUND", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }, "RootCheck").start();
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            setStatus("🟡 OVERLAY PERMISSION NEEDED", "#FFD700");
            appendLog("Requesting overlay permission…");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
        } else {
            appendLog("Overlay: OK");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                appendLog("Overlay granted!");
                setStatus("🟢 Ready to inject", "#00FF88");
            } else {
                setStatus("🔴 Overlay needed", "#FF3333");
            }
        }
    }

    private void launchAndInject() {
        if (!Settings.canDrawOverlays(this)) {
            checkOverlayPermission();
            return;
        }
        setStatus("🟡 Launching Subway Surfers…", "#FFD700");
        appendLog("Launching…");
        startService(new Intent(this, FloatingMenuService.class));

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(TARGET_PKG);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        } else {
            appendLog("ERROR: Game not installed!");
            setStatus("🔴 Game not installed!", "#FF3333");
            return;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!injected) performInjection();
            }
        }, 12000);
        appendLog("Waiting 12s for game load…");
    }

    private void injectOnly() {
        if (!Settings.canDrawOverlays(this)) {
            checkOverlayPermission();
            return;
        }
        startService(new Intent(this, FloatingMenuService.class));
        appendLog("Manual inject…");
        performInjection();
    }

    private void performInjection() {
        setStatus("🟡 INJECTING…", "#FFD700");
        appendLog("Extracting libs…");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File injectorFile = extractAsset("ffinjector", getFilesDir() + "/ffinjector");
                    File soFile = extractNativeLib("libsubwaybrutal.so", getFilesDir() + "/libsubwaybrutal.so");

                    if (injectorFile == null || soFile == null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("🔴 Extract failed!", "#FF3333");
                            }
                        });
                        return;
                    }

                    runShell("chmod 755 " + injectorFile.getAbsolutePath());
                    runShell("setenforce 0");

                    String pid = runShellOutput("pidof " + TARGET_PKG);
                    if (pid == null || pid.trim().isEmpty()) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("🔴 Game not running!", "#FF3333");
                                appendLog("No PID found");
                            }
                        });
                        return;
                    }
                    final String finalPid = pid.trim().split("\\s+")[0];
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            appendLog("PID: " + finalPid);
                        }
                    });

                    String injectCmd = injectorFile.getAbsolutePath() + " " + finalPid + " " + soFile.getAbsolutePath();
                    final String result = runShellOutput(injectCmd);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            appendLog("Injector: " + result);
                        }
                    });

                    injected = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setStatus("🟢 INJECTED!", "#00FF88");
                            appendLog("Done!");
                        }
                    });

                } catch (final Exception e) {
                    Log.e(TAG, "Inject err", e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setStatus("🔴 ERROR", "#FF3333");
                            appendLog("Ex: " + e.getMessage());
                        }
                    });
                }
            }
        }, "InjectorThread").start();
    }

    private File extractAsset(String assetName, String destPath) {
        try {
            InputStream in = getAssets().open(assetName);
            File out = new File(destPath);
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
            fos.close();
            in.close();
            appendLog("Extracted: " + assetName);
            return out;
        } catch (Exception e) {
            appendLog("Extract fail: " + assetName);
            return null;
        }
    }

    private File extractNativeLib(String libName, String destPath) {
        try {
            String installedLib = getApplicationInfo().nativeLibraryDir + "/" + libName;
            File installedFile = new File(installedLib);
            if (installedFile.exists()) {
                appendLog("Using installed: " + installedLib);
                return installedFile;
            }
            appendLog("Lib not found: " + libName);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void runShell(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Shell err: " + e.getMessage());
        }
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
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private void setStatus(final String msg, final String colorHex) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                statusText.setText(msg);
                statusText.setTextColor(Color.parseColor(colorHex));
            }
        });
    }

    private void appendLog(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (logText != null) logText.append("› " + msg + "\n");
            }
        });
    }
}
