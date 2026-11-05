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

    private static final long MAX_SESSION_MS = 600000L;

    private static final Pattern SPEED =
        Pattern.compile("speed\\s+10s/60s/15m\\s+(\\S+)");

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final AtomicBoolean requested = new AtomicBoolean(false);

    private volatile Process miningProcess;
    private volatile boolean closing = false;

    private TextView status;
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

        status = new TextView(this);
        status.setText(
            "\nNot mining\nHashrate: —\nAccepted shares: 0\n"
        );
        status.setTextSize(18);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

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

        setContentView(layout);
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

        acceptedShares = 0;
        speedValue = "Waiting for real hashrate";

        start.setEnabled(false);
        stop.setEnabled(true);
        wallet.setEnabled(false);

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

                args.add("--threads=5");
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

                    ui.postDelayed(() -> {

                        if (
                            miningProcess == running &&
                            requested.get()
                        ) {
                            stopMining(
                                "10-minute safety limit reached"
                            );
                        }

                    }, MAX_SESSION_MS);
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
                "Mining (5 threads)\n" +
                "Hashrate: " + speedValue + "\n" +
                "Accepted shares: " + acceptedShares +
                "\n" + shortLine
            );
        }
    }

    private void stopMining(String reason) {

        requested.set(false);

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
