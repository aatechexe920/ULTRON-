package com.ultron.bridge;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UltronNotificationListener extends NotificationListenerService {

    private static final String TAG = "ULTRON";
    private static final String BRIDGE_URL =
            "http://127.0.0.1:8765/notification";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

android.widget.Toast.makeText(
        this,
        "ULTRON received: " + sbn.getPackageName(),
        android.widget.Toast.LENGTH_LONG
).show();

        String packageName = sbn.getPackageName();

        Log.d(TAG, "Notification received from: " + packageName);

        Notification notification = sbn.getNotification();

        if (notification == null) {
            return;
        }

        Bundle extras = notification.extras;

        String title = "";
        String text = "";

        if (extras != null) {

            CharSequence titleValue =
                    extras.getCharSequence(Notification.EXTRA_TITLE);

            CharSequence textValue =
                    extras.getCharSequence(Notification.EXTRA_TEXT);

            if (titleValue != null) {
                title = titleValue.toString();
            }

            if (textValue != null) {
                text = textValue.toString();
            }
        }

        sendToTermux(packageName, title, text);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        Log.d(TAG,
                "Notification removed from: "
                        + sbn.getPackageName());
    }

    private void sendToTermux(
            String packageName,
            String title,
            String text) {

        final String finalPackage = packageName;
        final String finalTitle = title;
        final String finalText = text;

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(BRIDGE_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                String json =
                        "{"
                        + "\"package\":\""
                        + escapeJson(finalPackage)
                        + "\","
                        + "\"title\":\""
                        + escapeJson(finalTitle)
                        + "\","
                        + "\"text\":\""
                        + escapeJson(finalText)
                        + "\""
                        + "}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(json.getBytes("UTF-8"));
                output.flush();
                output.close();

                int responseCode =
                        connection.getResponseCode();

                Log.d(
                        TAG,
                        "Termux bridge response: "
                                + responseCode
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Could not send notification to Termux",
                        e
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
