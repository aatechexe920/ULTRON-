package com.ultron.bridge;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.LinearLayout;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private TextView floatingButton;
    private LinearLayout assistantPanel;
    private WindowManager.LayoutParams buttonParams;
    private WindowManager.LayoutParams panelParams;

    private static final String CHANNEL_ID = "ultron_service";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ULTRON is active")
                .setContentText("Floating assistant is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        createFloatingButton();
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createFloatingButton() {

        floatingButton = new TextView(this);

        floatingButton.setText("U");
        floatingButton.setTextSize(22);
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setTypeface(null, Typeface.BOLD);
        floatingButton.setGravity(Gravity.CENTER);
        floatingButton.setBackgroundColor(Color.rgb(0, 100, 220));
        floatingButton.setElevation(12);

        buttonParams = new WindowManager.LayoutParams(
                60,
                60,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = 20;
        buttonParams.y = 200;

        floatingButton.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float touchX;
            private float touchY;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        initialX = buttonParams.x;
                        initialY = buttonParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:

                        int dx = (int) (event.getRawX() - touchX);
                        int dy = (int) (event.getRawY() - touchY);

                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            moved = true;
                        }

                        buttonParams.x = initialX + dx;
                        buttonParams.y = initialY + dy;

                        windowManager.updateViewLayout(
                                floatingButton, buttonParams);

                        return true;

                    case MotionEvent.ACTION_UP:

                        if (!moved) {
                            togglePanel();
                        }

                        return true;
                }

                return false;
            }
        });

        windowManager.addView(floatingButton, buttonParams);
    }

    private void togglePanel() {

        if (assistantPanel != null) {
            windowManager.removeView(assistantPanel);
            assistantPanel = null;
            return;
        }

        assistantPanel = new LinearLayout(this);
        assistantPanel.setOrientation(LinearLayout.VERTICAL);
        assistantPanel.setPadding(30, 25, 30, 25);
        assistantPanel.setBackgroundColor(Color.rgb(10, 20, 50));
        assistantPanel.setElevation(20);

        TextView title = new TextView(this);
        title.setText("ULTRON AI ASSISTANT");
        title.setTextSize(20);
        title.setTextColor(Color.CYAN);
        title.setTypeface(null, Typeface.BOLD);

        TextView greeting = new TextView(this);
        greeting.setText("Hello! I'm ULTRON.\nHow can I help you today?");
        greeting.setTextSize(16);
        greeting.setTextColor(Color.WHITE);
        greeting.setPadding(0, 25, 0, 25);

        TextView close = new TextView(this);
        close.setText("CLOSE");
        close.setTextSize(16);
        close.setTextColor(Color.CYAN);
        close.setGravity(Gravity.CENTER);
        close.setPadding(10, 20, 10, 10);

        close.setOnClickListener(v -> togglePanel());

        assistantPanel.addView(title);
        assistantPanel.addView(greeting);
        assistantPanel.addView(close);

        panelParams = new WindowManager.LayoutParams(
                300,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.CENTER;

        windowManager.addView(assistantPanel, panelParams);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ULTRON Assistant",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {

        if (assistantPanel != null) {
            windowManager.removeView(assistantPanel);
            assistantPanel = null;
        }

        if (floatingButton != null) {
            windowManager.removeView(floatingButton);
            floatingButton = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
