package pl.zyper.autosms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat


class NotificationController(val context: Context) {
    var notificationID: Int = 3333131

    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var notification: Notification? = null

    fun build(text: String): Notification {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(context.getString(R.string.notification_default_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        notification = builder.build()

        return notification!!

    }

    fun show() {
        notificationManager.notify(notificationID, notification)
    }

    fun hide() {
        notificationManager.cancelAll()
    }


    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isNotificationVisible(): Boolean {

        val notificationIntent = Intent(context, MainActivity::class.java)
        val test = PendingIntent.getActivity(
            context,
            notificationID,
            notificationIntent,
            PendingIntent.FLAG_NO_CREATE
        )
        return test != null
    }


    companion object {
        const val CHANNEL_ID = "AutoSMSNotificationChannelID"
    }
}