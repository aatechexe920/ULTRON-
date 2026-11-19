package com.ultron.bridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Color;

public class MainActivity extends Activity {

private static final int OVERLAY_REQUEST = 100;
private static final int NOTIFICATION_REQUEST = 101;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setGravity(Gravity.CENTER);
    layout.setPadding(30, 30, 30, 30);

    TextView title = new TextView(this);
    title.setText("ULTRON");
    title.setTextSize(30);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);

    Button startButton = new Button(this);
    startButton.setText("ENABLE FLOATING BUTTON");

    startButton.setOnClickListener(v -> enableOverlay());

    layout.addView(title);
    layout.addView(startButton);

    setContentView(layout);
}

private void enableOverlay() {

    if (!Settings.canDrawOverlays(this)) {
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        );

        startActivityForResult(intent, OVERLAY_REQUEST);
        return;
    }

    if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {

        requestPermissions(
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            NOTIFICATION_REQUEST
        );
        return;
    }

    startOverlayService();
}

private void startOverlayService() {

    Intent service = new Intent(this, OverlayService.class);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(service);
    } else {
        startService(service);
    }
}

@Override
protected void onActivityResult(
    int requestCode, int resultCode, Intent data) {

    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == OVERLAY_REQUEST &&
            Settings.canDrawOverlays(this)) {

        enableOverlay();
    }
}

@Override
public void onRequestPermissionsResult(
    int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == NOTIFICATION_REQUEST) {
        startOverlayService();
    }
}

}
