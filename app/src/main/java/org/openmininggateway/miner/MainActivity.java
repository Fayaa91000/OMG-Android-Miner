package org.openmininggateway.miner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final long MAX_SESSION_MS = 7200000L;

    private static final Pattern SPEED =
        Pattern.compile("speed\\s+10s/60s/15m\\s+(\\S+)");

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final AtomicBoolean requested = new AtomicBoolean(false);

    private volatile Process miningProcess;
    private volatile boolean closing = false;

    private TextView status;
    private TextView countdown;
    private android.widget.Spinner threadSelector;
    private int selectedThreads = 3;
    private EditText wallet;
    private Button start;
    private Button stop;

    private int acceptedShares = 0;
    private String speedValue = "Waiting for miner";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 60, 40, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.rgb(14, 22, 35));

        android.widget.ImageView logo =
            new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.omg_logo);
        int logoSize = (int) (90 *
            getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams logoParams =
            new LinearLayout.LayoutParams(logoSize, logoSize);
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        layout.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("OMG MINER");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        layout.addView(title);

        TextView information = new TextView(this);
        information.setText(
            "\nMonero (RandomX Light) | HashVault TLS\n" +
            "CPU mining uses battery and may heat your phone.\n" +
            "Mining stops if this screen closes.\n"
        );
        information.setGravity(Gravity.CENTER);
        information.setTextColor(Color.LTGRAY);
        layout.addView(information);

        wallet = new EditText(this);
        wallet.setSingleLine(true);
        wallet.setTextColor(Color.WHITE);
        wallet.setHintTextColor(Color.LTGRAY);
        wallet.setHint("Your own Monero wallet address");
        layout.addView(wallet);

        Button pasteWallet = new Button(this);
        pasteWallet.setText("PASTE WALLET ADDRESS");
        layout.addView(pasteWallet);

        pasteWallet.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);

            if (clipboard == null ||
                !clipboard.hasPrimaryClip()) {
                android.widget.Toast.makeText(
                    this, "Clipboard is empty",
                    android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            android.content.ClipData clip =
                clipboard.getPrimaryClip();

            if (clip == null || clip.getItemCount() == 0) {
                return;
            }

            CharSequence copied =
                clip.getItemAt(0).coerceToText(this);

            if (copied != null) {
                wallet.setText(copied.toString().trim());
                wallet.setSelection(wallet.length());
            }
        });

        TextView threadLabel = new TextView(this);
        threadLabel.setText("CPU threads (1–6, default 3)");
        threadLabel.setTextColor(Color.WHITE);
        threadLabel.setTextSize(16);
        layout.addView(threadLabel);

        int cpuLimit = Math.min(6, Math.max(1,
            Runtime.getRuntime().availableProcessors()));

        String[] threadOptions = new String[cpuLimit];
        for (int i = 0; i < cpuLimit; i++) {
            threadOptions[i] = (i + 1) + " threads";
        }

        threadSelector = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter =
            new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                threadOptions
            );
        threadSelector.setAdapter(adapter);
        threadSelector.setSelection(Math.min(2, cpuLimit - 1));
        layout.addView(threadSelector);

        status = new TextView(this);
        status.setText(
            "\nNot mining\nHashrate: —\nAccepted shares: 0\n"
        );
        status.setTextSize(18);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        countdown = new TextView(this);
        countdown.setText("Session duration: 02:00:00");
        countdown.setTextColor(Color.WHITE);
        countdown.setTextSize(20);
        countdown.setGravity(Gravity.CENTER);
        countdown.setPadding(0, 16, 0, 16);
        layout.addView(countdown);

        start = new Button(this);
        start.setText("START MINING");
        start.setTextColor(Color.WHITE);
        layout.addView(start);

        stop = new Button(this);
        stop.setText("STOP MINING");
        stop.setTextColor(Color.WHITE);
        stop.setEnabled(false);
        layout.addView(stop);

        start.setOnClickListener(v -> beginMining());

        stop.setOnClickListener(
            v -> stopMining("Stopped by user")
        );

        Button github = new Button(this);
        github.setText("OMG GitHub");
        layout.addView(github);

        github.setOnClickListener(v -> {
            android.content.Intent intent =
                new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://github.com/Fayaa91000/OMG-Android-Miner"
                    )
                );
            startActivity(intent);
        });

        Button earnings = new Button(this);
        earnings.setText("CHECK MINING EARNINGS");
        layout.addView(earnings);

        earnings.setOnClickListener(v -> {
            android.content.Intent intent =
                new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://hashvault.pro/monero/"
                    )
                );
            startActivity(intent);
        });

        TextView developer = new TextView(this);
        developer.setText("Developed by Hassanaat Gems");
        developer.setTextColor(Color.LTGRAY);
        developer.setTextSize(15);
        developer.setGravity(Gravity.CENTER);
        developer.setPadding(0, 24, 0, 12);
        layout.addView(developer);

        android.widget.ScrollView scroll =
            new android.widget.ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void beginMining() {

        if (!requested.compareAndSet(false, true)) {
            return;
        }

        String address = wallet.getText().toString().trim();

        if (address.length() < 90 || address.contains(" ")) {
            requested.set(false);
            status.setText(
                "Enter your own valid Monero wallet address first."
            );
            return;
        }

        String safety = safetyIssue();
        if (safety != null) {
            requested.set(false);
            status.setText(safety);
            return;
        }

        selectedThreads = threadSelector.getSelectedItemPosition() + 1;
        acceptedShares = 0;
        speedValue = "Waiting for real hashrate";

        start.setEnabled(false);
        stop.setEnabled(true);
        wallet.setEnabled(false);
        threadSelector.setEnabled(false);

        status.setText(
            "Starting XMRig...\n" +
            "Hashrate: —\n" +
            "Accepted shares: 0"
        );

        new Thread(() -> {

            Process process = null;

            try {

                File miner = new File(
                    getApplicationInfo().nativeLibraryDir,
                    "libxmrig.so"
                );

                if (!miner.isFile() || !miner.canExecute()) {
                    throw new IllegalStateException(
                        "Packaged XMRig executable unavailable: " +
                        miner.getAbsolutePath()
                    );
                }

                List<String> args = new ArrayList<>();

                args.add(miner.getAbsolutePath());
                args.add("-o");
                args.add("pool.hashvault.pro:443");

                args.add("--tls");

                args.add("--coin");
                args.add("monero");

                args.add("--algo");
                args.add("rx/0");

                args.add("-u");
                args.add(address);

                args.add("-p");
                args.add("OMG-Android");

                args.add("--threads=" + selectedThreads);
                args.add("--randomx-mode=light");
                args.add("--print-time=10");
                args.add("--no-color");
                args.add("--no-huge-pages");

                ProcessBuilder pb = new ProcessBuilder(args);
                pb.redirectErrorStream(true);
                pb.directory(getFilesDir());

                process = pb.start();
                miningProcess = process;

                final Process running = process;

                if (!requested.get()) {
                    running.destroy();
                } else {
                    final long startedAt =
                        android.os.SystemClock.elapsedRealtime();

                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            if (closing || !requested.get()
                                || miningProcess != running) {
                                return;
                            }

                            String safety = safetyIssue();
                            if (safety != null) {
                                stopMining(safety);
                                return;
                            }

                            long elapsed =
                                android.os.SystemClock.elapsedRealtime()
                                - startedAt;

                            long remaining = Math.max(
                                0L, MAX_SESSION_MS - elapsed
                            );

                            long seconds = (remaining + 999) / 1000;

                            countdown.setText(String.format(
                                java.util.Locale.US,
                                "Time remaining: %02d:%02d:%02d",
                                seconds / 3600,
                                (seconds / 60) % 60,
                                seconds % 60
                            ));

                            if (remaining == 0L) {
                                stopMining("2-hour session completed");
                                return;
                            }

                            ui.postDelayed(this, 1000);
                        }
                    });
                }

                try (
                    BufferedReader lines = new BufferedReader(
                        new InputStreamReader(
                            process.getInputStream()
                        )
                    )
                ) {

                    String line;

                    while ((line = lines.readLine()) != null) {

                        if (!requested.get()) {
                            continue;
                        }

                        final String output = line;

                        ui.post(
                            () -> showMiningOutput(output)
                        );
                    }
                }

                int result = process.waitFor();
                final int exitCode = result;

                ui.post(() -> {

                    if (
                        miningProcess == running &&
                        requested.get()
                    ) {

                        status.setText(
                            "Miner exited (code " +
                            exitCode + ").\n" +
                            "Hashrate: " + speedValue + "\n" +
                            "Accepted shares: " + acceptedShares
                        );
                    }
                });

            } catch (Exception ex) {

                String message =
                    ex.getMessage() == null
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();

                ui.post(() -> {

                    if (!closing && requested.get()) {

                        status.setText(
                            "Could not start XMRig:\n" + message
                        );
                    }
                });

            } finally {

                if (
                    process != null &&
                    miningProcess == process
                ) {
                    miningProcess = null;
                }

                requested.set(false);

                ui.post(() -> {

                    if (!closing) {
                        setIdle();
                    }

                });
            }

        }, "omg-miner-worker").start();
    }

    private void showMiningOutput(String line) {

        if (closing || !requested.get()) {
            return;
        }

        Matcher m = SPEED.matcher(line);

        if (m.find()) {
            speedValue = m.group(1) + " H/s (10s)";
        }

        if (line.contains("accepted (")) {
            acceptedShares++;
        }

        if (
            m.find(0) ||
            line.contains("accepted (") ||
            line.contains("new job") ||
            line.contains("error")
        ) {

            String shortLine =
                line.length() > 110
                    ? line.substring(0, 110)
                    : line;

            status.setText(
                "Mining (" + selectedThreads + " threads)\n" +
                "Hashrate: " + speedValue + "\n" +
                "Accepted shares: " + acceptedShares +
                "\n" + shortLine
            );
        }
    }

    private String safetyIssue() {
        android.content.Intent battery = registerReceiver(
            null,
            new android.content.IntentFilter(
                android.content.Intent.ACTION_BATTERY_CHANGED
            )
        );

        if (battery != null) {
            int level = battery.getIntExtra(
                android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(
                android.os.BatteryManager.EXTRA_SCALE, -1);
            int temp = battery.getIntExtra(
                android.os.BatteryManager.EXTRA_TEMPERATURE, -1);

            if (level >= 0 && scale > 0 &&
                level * 100 / scale < 20) {
                return "Safety stop: battery below 20%";
            }

            if (temp >= 430) {
                return "Safety stop: high battery temperature";
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.os.PowerManager power =
                (android.os.PowerManager)
                getSystemService(POWER_SERVICE);

            if (power != null &&
                power.getCurrentThermalStatus() >=
                android.os.PowerManager.THERMAL_STATUS_SEVERE) {
                return "Safety stop: device overheating";
            }
        }

        return null;
    }

    private void stopMining(String reason) {

        requested.set(false);
        countdown.setText("Session stopped");

        Process process = miningProcess;

        status.setText(
            reason + "\n" +
            "Last hashrate: " + speedValue + "\n" +
            "Accepted shares: " + acceptedShares
        );

        start.setEnabled(false);
        stop.setEnabled(false);

        if (process != null) {

            process.destroy();

            new Thread(() -> {

                try {

                    Thread.sleep(3000);

                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }

                } catch (InterruptedException ignored) {

                    Thread.currentThread().interrupt();
                }

            }, "omg-miner-stop").start();
        }
    }

    private void setIdle() {

        start.setEnabled(true);
        stop.setEnabled(false);
        wallet.setEnabled(true);
        threadSelector.setEnabled(true);
    }

    @Override
    protected void onDestroy() {

        closing = true;

        stopMining(
            "Mining stopped: app screen closed"
        );

        super.onDestroy();
    }
}
