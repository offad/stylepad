package com.davidfadare.notes.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.davidfadare.notes.MainActivity
import com.davidfadare.notes.R
import com.davidfadare.notes.settings.SettingsActivity


class AlarmService : JobIntentService() {

    companion object {
        const val JOB_ID = 1000
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, AlarmService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val notificationId = intent.getIntExtra(getString(R.string.note_alarm), 0)
        val title = intent.getStringExtra(getString(R.string.note_title))
        val text = intent.getStringExtra(getString(R.string.note_text))
        val color = intent.getIntExtra(getString(R.string.note_color), ContextCompat.getColor(this, R.color.blue_note))
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, tapIntent, 0)

        sendNotification(notificationId, title, text, color, pendingIntent)
    }

    private fun sendNotification(notificationId: Int, title: String?, text: String?, color: Int, pendingIntent: PendingIntent) {
        val builder = NotificationCompat.Builder(this, getString(R.string.editor_alarm_channel_name))
                .setSmallIcon(R.drawable.notification_image)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setColor(color)
                .setLights(-0x48e3e4, 1000, 2000)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val ringtonePreferenceOption = sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_NOTIFICATION, true)
        if (ringtonePreferenceOption) {
            val alarmVibrate = sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_VIBRATE, true)
            val alarmSound = sharedPreferences.getString(SettingsActivity.KEY_PREF_RINGTONE, "")

            if (alarmVibrate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channel = notificationManager.getNotificationChannel(getString(R.string.editor_alarm_channel_name))
                    channel.vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
                }
                builder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channel = notificationManager.getNotificationChannel(getString(R.string.editor_alarm_channel_name))
                    channel.vibrationPattern = longArrayOf(0L)
                }
                builder.setVibrate(longArrayOf(0L))
            }
            val sound: Uri = if (!TextUtils.isEmpty(alarmSound)) {
                Uri.parse(alarmSound)
            } else {
                Settings.System.DEFAULT_NOTIFICATION_URI
            }
            builder.setSound(sound)
        }

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

}
