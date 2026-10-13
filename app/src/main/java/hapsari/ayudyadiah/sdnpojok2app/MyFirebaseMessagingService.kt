package hapsari.ayudyadiah.sdnpojok2app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_LOG", "Token Baru: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Ambil data baik dari payload 'notification' maupun 'data'
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Notifikasi Baru"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: remoteMessage.data["body"] ?: ""

        if (body.isNotEmpty()) {
            tampilkanNotifikasi(title, body)
        }
    }

    private fun tampilkanNotifikasi(title: String, message: String) {
        val channelId = "ppdb_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notifikasi PPDB", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Gunakan ID unik agar notifikasi tidak saling menimpa
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}