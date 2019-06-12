package com.tistory.mybstory.webrtc_example_kt.ui

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.databinding.DataBindingUtil
import com.tistory.mybstory.webrtc_example_kt.R
import com.tistory.mybstory.webrtc_example_kt.databinding.ActivityCallBinding
import com.tistory.mybstory.webrtc_example_kt.service.RtcService
import org.webrtc.RendererCommon
import timber.log.Timber

class CallActivity : Activity() {

    private lateinit var remoteUID: String
    private lateinit var binding: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        intent!!.getStringExtra("remoteUID")?.let{
            remoteUID = it
            Timber.d("Remote uid : $remoteUID")
        }

        RtcService.bindService(applicationContext, serviceConnection)
    }

    val serviceConnection =  object: ServiceConnection {

        override fun onServiceDisconnected(p0: ComponentName?) {

        }

        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
            val binder = iBinder as RtcService.LocalBinder
            Timber.e("Service bound on call activity!!")
            onWebRtcConnected(binder.getService())
        }
    }

    private fun onWebRtcConnected(service: RtcService) {
        service.run {
            attachLocalView(binding.localRenderer)
            attachRemoteView(binding.remoteRenderer)
            if (intent!!.getBooleanExtra("isCaller", false)) {
                offerDevice(remoteUID)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        updateViews()
    }

    override fun onDestroy() {
        binding.localRenderer.release()
        binding.remoteRenderer.release()
        applicationContext.unbindService(serviceConnection)
        super.onDestroy()
    }

    private fun updateViews() {
        binding.localRenderer.run {
            setMirror(true)
            setZOrderOnTop(true)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            requestLayout()
        }

        binding.remoteRenderer.run {
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setMirror(false)
            requestLayout()
        }
    }
}