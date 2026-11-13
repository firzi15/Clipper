package com.clipper.app

import android.app.Application

class ClipperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ClipperApp
            private set
    }
}
