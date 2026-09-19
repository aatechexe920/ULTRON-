package com.ultron.bridge;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class UltronNotificationListener extends NotificationListenerService {

    private static final String TAG = "ULTRON";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        String packageName = sbn.getPackageName();

        Log.d(TAG, "Notification received from: " + packageName);

        // Notification content is intentionally not logged yet.
        // We will add explicit app filtering and event handling later.
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        String packageName = sbn.getPackageName();

        Log.d(TAG, "Notification removed from: " + packageName);
    }
}
