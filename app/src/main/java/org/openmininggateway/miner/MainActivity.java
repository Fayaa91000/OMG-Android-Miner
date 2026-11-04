package org.openmininggateway.miner;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
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

        TextView status = new TextView(this);
        status.setText("\nMining engine not connected\nHashrate: 0 H/s\n");
        status.setTextSize(18);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        Button start = new Button(this);
        start.setText("START MINING");
        start.setEnabled(false);
        start.setTextColor(Color.WHITE);
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("STOP MINING");
        stop.setEnabled(false);
        stop.setTextColor(Color.WHITE);
        layout.addView(stop);

        setContentView(layout);
    }
}
