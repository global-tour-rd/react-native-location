package com.github.reactnativecommunity.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RNLocationForegroundService extends Service {
    private static final String CHANNEL_ID = "RNLocationForegroundService";
    private static final int NOTIFICATION_ID = 1001;
    private static RNLocationProvider locationProvider;
    private static boolean locationProviderRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (locationProvider != null) {
            locationProviderRunning = true;
            locationProvider.startUpdatingLocation();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (locationProvider != null) {
            locationProviderRunning = false;
            locationProvider.stopUpdatingLocation();
        }

        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service Running")
            .setContentText("Tracking location in the background")
            .setPriority(NotificationCompat.PRIORITY_LOW);

        int iconSource = getResourceIdForResourceName("ic_launcher");
        notificationBuilder.setSmallIcon(iconSource);

        return notificationBuilder.build();
    }

    private int getResourceIdForResourceName(String resourceName) {
        int resourceId = this.getResources().getIdentifier(resourceName, "drawable", this.getPackageName());
        if (resourceId == 0) {
            resourceId = this.getResources().getIdentifier(resourceName, "mipmap", this.getPackageName());
        }
        return resourceId;
    }

    public static void setLocationProvider(RNLocationProvider provider) {
        locationProvider = provider;

        if (locationProviderRunning) {
            locationProvider.stopUpdatingLocation();
            locationProvider.startUpdatingLocation();
        }
    }
}
