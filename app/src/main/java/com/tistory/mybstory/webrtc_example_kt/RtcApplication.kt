package com.tistory.mybstory.webrtc_example_kt

import android.app.Application
import android.content.Context
import com.tistory.mybstory.webrtc_example_kt.service.RtcService
import timber.log.Timber

class RtcApplication : Application() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        RtcService.startService(applicationContext)
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    companion object {
        private var instance: RtcApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

}