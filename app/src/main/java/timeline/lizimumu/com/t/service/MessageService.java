package timeline.lizimumu.com.t.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import timeline.lizimumu.com.t.R;
import timeline.lizimumu.com.t.ui.MainActivity;

/**
 * on message
 * Created by zb on 03/01/2018.
 */

public class MessageService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Intent resultIntent = new Intent(this, MainActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            // 这个是应用在前台的时候出现的通知，应用在后台不会调用，这个并不能把应用拉起来的
            Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Notification n = new NotificationCompat.Builder(this, "channel.fcm")
                    .setContentIntent(resultPendingIntent)
                    .setContentTitle(data.get("title"))
                    .setContentText(data.get("content"))
                    .setSound(notificationSound)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) notificationManager.notify(1, n);
        }
    }
}
