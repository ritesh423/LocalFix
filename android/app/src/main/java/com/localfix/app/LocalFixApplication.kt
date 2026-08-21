package com.localfix.app

import android.app.Application
import com.localfix.app.data.DefaultAppContainer
import com.localfix.app.notifications.NotificationChannels

class LocalFixApplication : Application() {
    val appContainer by lazy { DefaultAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }
}
