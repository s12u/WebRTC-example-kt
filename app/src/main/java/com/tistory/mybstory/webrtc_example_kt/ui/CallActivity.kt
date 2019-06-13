package com.tistory.mybstory.webrtc_example_kt.ui

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.telecom.Call
import android.view.View
import androidx.databinding.DataBindingUtil
import com.tistory.mybstory.webrtc_example_kt.R
import com.tistory.mybstory.webrtc_example_kt.service.CallHandler
import com.tistory.mybstory.webrtc_example_kt.data.model.CallEvent
import com.tistory.mybstory.webrtc_example_kt.databinding.ActivityCallBinding
import com.tistory.mybstory.webrtc_example_kt.service.RtcService
import io.reactivex.disposables.Disposable
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import timber.log.Timber

class CallActivity : Activity() {

    private var service: RtcService? = null
    private var callStateDisposable: Disposable? = null
    private lateinit var remoteUID: String
    private lateinit var binding: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        intent!!.getStringExtra("remoteUID")?.let {
            remoteUID = it
            Timber.d("Remote uid : $remoteUID")
        }
        RtcService.bindService(applicationContext, serviceConnection)
    }

    val serviceConnection = object : ServiceConnection {

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
            this@CallActivity.service = service
            attachLocalView(binding.localRenderer)
            attachRemoteView(binding.remoteRenderer)

            if (intent!!.getBooleanExtra("isCaller", false)) {
                offerDevice(remoteUID)
                binding.buttonAnswerCall.visibility = View.GONE
                binding.buttonHangUp.visibility = View.VISIBLE
            }
        }

        callStateDisposable = CallHandler.getInstance().callback
            .filter { it.type == CallEvent.Type.STATE_CHANGED }.subscribe {
                when (it.iceConnectionState) {
                    PeerConnection.IceConnectionState.CHECKING -> {

                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        finish()
                    }
                    else -> {
                    }
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
        callStateDisposable?.dispose()
        applicationContext.unbindService(serviceConnection)
        service = null
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

        binding.buttonAnswerCall.setOnClickListener {
            acceptCall()
        }

        binding.buttonHangUp.setOnClickListener {
            hangUpCall()
        }
    }

    private fun acceptCall() {
        CallHandler.getInstance().onActionPerformed(CallEvent.CallAction.ACCEPT)
        binding.buttonAnswerCall.visibility = View.GONE
        binding.buttonHangUp.visibility = View.VISIBLE
    }

    private fun hangUpCall() {
        CallHandler.getInstance().onActionPerformed(CallEvent.CallAction.HANG_UP)
        finish()
    }

}