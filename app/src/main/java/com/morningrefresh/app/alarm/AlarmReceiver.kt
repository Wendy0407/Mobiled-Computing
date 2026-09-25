package com.morningrefresh.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.morningrefresh.app.MainActivity
import com.morningrefresh.app.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Morning alarm"
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        val openApp = PendingIntent.getActivity(
            context,
            901,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_CHECK_IN, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("Good morning. Complete a short check-in to refresh your plan.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        val notifications = NotificationManagerCompat.from(context)
        if (notifications.areNotificationsEnabled()) {
            notifications.notify(NOTIFICATION_ID, notification)
        }
        if (alarmId > 0 && hour in 0..23 && minute in 0..59) {
            AlarmScheduler(context).schedule(
                com.morningrefresh.app.data.AlarmEntity(
                    id = alarmId,
                    label = label,
                    hour = hour,
                    minute = minute,
                    enabled = true,
                ),
            )
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Morning alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Morning Refresh alarm notifications" },
        )
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_HOUR = "alarm_hour"
        const val EXTRA_MINUTE = "alarm_minute"
        const val CHANNEL_ID = "morning_alarm_channel"
        private const val NOTIFICATION_ID = 4001
    }
}
