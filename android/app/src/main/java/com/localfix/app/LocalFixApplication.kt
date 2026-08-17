package com.localfix.app

import android.app.Application
import com.localfix.app.data.DefaultAppContainer

class LocalFixApplication : Application() {
    val appContainer by lazy { DefaultAppContainer(this) }
}
