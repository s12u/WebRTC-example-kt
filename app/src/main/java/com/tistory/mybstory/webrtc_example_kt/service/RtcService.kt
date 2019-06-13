package com.tistory.mybstory.webrtc_example_kt.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber

class RtcService : Service() {

    private val serviceController: RtcServiceController by lazy { RtcServiceController() }
    private lateinit var localBinder: LocalBinder

    override fun onCreate() {
        super.onCreate()
        Timber.e("RtcService onCreate()")
        localBinder = LocalBinder()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceController.detachService()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(p0: Intent?) = localBinder

    override fun onUnbind(intent: Intent?): Boolean {
        //TODO: need to reset rtc client
        //serviceController.detachService()
        serviceController.resetRtcClient()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceController.attachService(this)
        return START_NOT_STICKY
    }

    companion object {
        fun startServiceWithContext(context: Context) = context.startService(Intent(context, RtcService::class.java))!!
        fun bindService(context: Context, serviceConnection: ServiceConnection) =
            context.bindService(Intent(context, RtcService::class.java), serviceConnection, 0)

    }

    inner class LocalBinder : Binder() {
        fun getService(): RtcService = this@RtcService
    }

    fun attachLocalView(localRenderer: SurfaceViewRenderer) = serviceController.attachLocalView(localRenderer)

    fun attachRemoteView(remoteRenderer: SurfaceViewRenderer) = serviceController.attachRemoteView(remoteRenderer)

    fun offerDevice(remoteUid: String) = serviceController.offerDevice(remoteUid)

}