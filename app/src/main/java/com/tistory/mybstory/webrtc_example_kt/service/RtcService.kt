package com.tistory.mybstory.webrtc_example_kt.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import com.tistory.mybstory.webrtc_example_kt.RtcApplication

class RtcService: Service() {

    private lateinit var serviceController: RtcServiceController
    private lateinit var localBinder: LocalBinder

    override fun onCreate() {
        super.onCreate()
        localBinder = LocalBinder()
        serviceController = RtcServiceController(RtcApplication.applicationContext())
        serviceController.attachService(this)
    }

    override fun onDestroy() {
        serviceController.detachService()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?) = localBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        fun startService(context: Context) = context.startService(Intent(context, RtcService::class.java))
        fun bindService(context: Context, serviceConnection: ServiceConnection)
                = context.bindService(Intent(context, RtcService::class.java), serviceConnection, 0)
    }

    inner class LocalBinder: Binder() {
        fun getService() : RtcService = RtcService()
    }

}