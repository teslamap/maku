package com.makasia.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MakasiaMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "makasia_orders";
    private static final AtomicInteger notificationIds = new AtomicInteger(1000);

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // MainActivity obtains the current token when the app starts and forwards it
        // to the authenticated WebView, where Firestore rules restrict writes to self.
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();
        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : (data.containsKey("title") ? data.get("title") : "ახალი შეკვეთა");
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : (data.containsKey("body") ? data.get("body") : "მიღებულია ახალი შეკვეთა");
        showNotification(title, body, data.get("orderId"));
    }

    private void showNotification(String title, String body, String orderId) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Makasia შეკვეთები", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("ახალი შეკვეთების შეტყობინებები");
            manager.createNotificationChannel(channel);
        }

        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) launch = new Intent(this, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (orderId != null) launch.putExtra("orderId", orderId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, orderId == null ? notificationIds.get() : orderId.hashCode(), launch,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(getApplicationInfo().icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(this).notify(notificationIds.incrementAndGet(), builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ notification permission may be denied by the user.
        }
    }
}