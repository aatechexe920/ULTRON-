package com.ultron.bridge;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UltronNotificationListener extends NotificationListenerService {

    private static final String TAG = "ULTRON";

    private static final String BRIDGE_URL =
            "http://127.0.0.1:8765/notification";

    /*
     * Remembers what ULTRON has already seen for each Android
     * notification.
     *
     * Key = app + notification ID + notification tag
     */
    private final Map<String, Set<String>> previousContent =
            new HashMap<>();


    @Override
    public void onListenerConnected() {

        super.onListenerConnected();

        Log.d(TAG, "ULTRON listener CONNECTED");
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        try {

            Notification notification = sbn.getNotification();

            if (notification == null) {
                return;
            }

            Bundle extras = notification.extras;

            if (extras == null) {
                return;
            }


            String packageName = sbn.getPackageName();

            String title = "";

            CharSequence titleValue =
                    extras.getCharSequence(Notification.EXTRA_TITLE);

            if (titleValue != null) {
                title = titleValue.toString();
            }


            /*
             * Build a list containing the actual notification
             * content lines.
             */
            List<String> currentLines =
                    extractNotificationLines(extras);


            if (currentLines.isEmpty()) {

                CharSequence textValue =
                        extras.getCharSequence(Notification.EXTRA_TEXT);

                if (textValue != null) {

                    String text = textValue.toString().trim();

                    if (!text.isEmpty()) {
                        currentLines.add(text);
                    }
                }
            }


            /*
             * Unique key for this Android notification.
             */
            String notificationKey =
                    packageName
                            + "|"
                            + sbn.getId()
                            + "|"
                            + String.valueOf(sbn.getTag());


            /*
             * Get what ULTRON saw previously.
             */
            Set<String> oldLines =
                    previousContent.get(notificationKey);


            /*
             * First time seeing this notification:
             * send the current content.
             */
            if (oldLines == null) {

                oldLines = new HashSet<>();

                previousContent.put(
                        notificationKey,
                        oldLines
                );

                for (String line : currentLines) {

                    if (!line.isEmpty()) {
                        oldLines.add(normalize(line));
                    }
                }

                sendNotification(
                        packageName,
                        title,
                        joinLines(currentLines)
                );

                return;
            }


            /*
             * Notification already existed.
             *
             * Find ONLY lines that weren't present before.
             */
            List<String> newLines =
                    new ArrayList<>();


            for (String line : currentLines) {

                String normalized =
                        normalize(line);

                if (normalized.isEmpty()) {
                    continue;
                }

                if (!oldLines.contains(normalized)) {

                    newLines.add(line);

                    oldLines.add(normalized);
                }
            }


            /*
             * Nothing new was added.
             *
             * This prevents Android from repeatedly sending
             * the same old notification contents to ULTRON.
             */
            if (newLines.isEmpty()) {

                Log.d(
                        TAG,
                        "No new notification content: "
                                + packageName
                );

                return;
            }


            /*
             * Send ONLY the new notification content.
             */
            sendNotification(
                    packageName,
                    title,
                    joinLines(newLines)
            );


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Notification processing error",
                    e
            );
        }
    }


    /*
     * Extract notification lines.
     *
     * Android apps such as Telegram and WhatsApp can use
     * notification styles that contain multiple lines.
     */
    private List<String> extractNotificationLines(
            Bundle extras
    ) {

        List<String> lines =
                new ArrayList<>();


        /*
         * Standard Android notification lines.
         */
        CharSequence[] textLines =
                extras.getCharSequenceArray(
                        Notification.EXTRA_TEXT_LINES
                );


        if (textLines != null) {

            for (CharSequence value : textLines) {

                if (value == null) {
                    continue;
                }

                String text =
                        value.toString().trim();

                if (!text.isEmpty()) {
                    lines.add(text);
                }
            }
        }


        /*
         * If EXTRA_TEXT_LINES wasn't available,
         * use the normal notification text.
         */
        if (lines.isEmpty()) {

            CharSequence text =
                    extras.getCharSequence(
                            Notification.EXTRA_TEXT
                    );

            if (text != null) {

                String value =
                        text.toString().trim();

                if (!value.isEmpty()) {
                    lines.add(value);
                }
            }
        }


        return lines;
    }


    /*
     * Normalize text so tiny formatting differences don't
     * make Android's old notification look like a new one.
     */
    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        return text
                .trim()
                .replaceAll("\\s+", " ");
    }


    /*
     * Join multiple new notification lines into one message.
     */
    private String joinLines(List<String> lines) {

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            String value =
                    line.trim();

            if (value.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append("\n");
            }

            result.append(value);
        }

        return result.toString();
    }


    /*
     * Send the NEW content to the Termux bridge.
     */
    private void sendNotification(
            String packageName,
            String title,
            String text
    ) {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

            try {

                JSONObject json =
                        new JSONObject();

                json.put(
                        "package",
                        packageName
                );

                json.put(
                        "title",
                        title
                );

                json.put(
                        "text",
                        text
                );


                URL url =
                        new URL(BRIDGE_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "POST"
                );

                connection.setConnectTimeout(
                        3000
                );

                connection.setReadTimeout(
                        3000
                );

                connection.setDoOutput(
                        true
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );


                byte[] data =
                        json.toString()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                );


                try (OutputStream output =
                             connection.getOutputStream()) {

                    output.write(data);
                    output.flush();
                }


                int responseCode =
                        connection.getResponseCode();


                Log.d(
                        TAG,
                        "Notification sent. HTTP "
                                + responseCode
                );


            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Failed to send notification",
                        e
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}
