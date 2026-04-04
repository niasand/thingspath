package com.thingspath

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThingsPathApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
