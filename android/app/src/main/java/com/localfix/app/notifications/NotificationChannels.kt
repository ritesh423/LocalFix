package com.localfix.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.localfix.app.R

object NotificationChannels {
    const val UPDATES_CHANNEL_ID = "localfix_updates"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                UPDATES_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            },
        )
    }
}
