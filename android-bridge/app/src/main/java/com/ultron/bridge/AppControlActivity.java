package com.ultron.bridge;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class AppControlActivity extends Activity {

    private static final String TAG = "ULTRON_APP_CONTROL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getStringExtra("action");

        if ("discover".equals(action)) {
            discoverApps();
        } else if ("launch".equals(action)) {
            String name = getIntent().getStringExtra("app_name");
            launchApp(name);
        } else {
            Log.e(TAG, "Unknown or missing action.");
        }

        finish();
    }

    private void discoverApps() {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = getPackageManager();

        List<ResolveInfo> apps =
                pm.queryIntentActivities(intent, 0);

        Log.i(TAG, "INSTALLED LAUNCHABLE APPS:");

        for (ResolveInfo app : apps) {

            String name = app.loadLabel(pm).toString();
            String packageName =
                    app.activityInfo.packageName;

            Log.i(TAG, name + " | " + packageName);
        }

        Log.i(TAG, "TOTAL APPS: " + apps.size());
    }

    private void launchApp(String requestedName) {

        if (requestedName == null || requestedName.trim().isEmpty()) {
            Log.e(TAG, "No app name supplied.");
            return;
        }

        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps =
                pm.queryIntentActivities(intent, 0);

        for (ResolveInfo app : apps) {

            String name = app.loadLabel(pm).toString();

            if (name.equalsIgnoreCase(requestedName.trim())) {

                String packageName =
                        app.activityInfo.packageName;

                Intent launchIntent =
                        pm.getLaunchIntentForPackage(packageName);

                if (launchIntent != null) {

                    launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    );

                    try {
                        startActivity(launchIntent);
                        Log.i(TAG, "Successfully launched: " + name);
                    } catch (Exception e) {
                        Log.e(TAG, "Launch failed: " + name, e);
                    }

                    return;
                }
            }
        }

        Log.e(TAG, "App not found: " + requestedName);
    }
}
