package com.tistory.mybstory.webrtc_example_kt.ui

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.databinding.DataBindingUtil
import com.tistory.mybstory.webrtc_example_kt.R
import com.tistory.mybstory.webrtc_example_kt.data.model.CallEvent
import com.tistory.mybstory.webrtc_example_kt.databinding.ActivityCallBinding
import com.tistory.mybstory.webrtc_example_kt.service.CallHandler
import com.tistory.mybstory.webrtc_example_kt.service.RtcService
import io.reactivex.disposables.Disposable
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import timber.log.Timber


// TODO: need to make <Back button> disabled
class CallActivity : Activity() {

    private var service: RtcService? = null
    private val callHandler by lazy { CallHandler.getInstance() }
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
            service.attachRemoteView(binding.remoteRenderer)

            if (intent!!.getBooleanExtra("isCaller", false)) {
                offerDevice(remoteUID)
                binding.buttonHangUp.visibility = View.VISIBLE
                binding.buttonAnswerCall.visibility = View.GONE
                binding.buttonRefuseCall.visibility = View.GONE
            }
        }

        callStateDisposable = callHandler.callback
            .filter { it.type == CallEvent.Type.STATE_CHANGED }.subscribe(
                {
                    when (it.iceConnectionState) {
                        PeerConnection.IceConnectionState.CONNECTED -> {

                        }
                        PeerConnection.IceConnectionState.CLOSED -> {
                            hangUpCall()
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            hangUpCall()
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            showAlertDialog()
                        }
                        else -> {

                        }

                    }
                }, {

                }
            )
    }

    override fun onStart() {
        super.onStart()
        updateViews()
    }

    override fun onDestroy() {
        callStateDisposable?.dispose()
        RtcService.unbindService(applicationContext, serviceConnection)
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
            setMirror(true)
            requestLayout()
        }

        binding.layoutMotionCall.setTransitionListener(transitionListener)

        binding.buttonRefuseCall.setOnClickListener {
            refuseCall()
        }

        binding.buttonHangUp.setOnClickListener {
            hangUpCall()
        }

        binding.buttonSwitchCamera.setOnClickListener {
            switchCamera()
        }
    }

    private fun acceptCall() {
        callHandler.onActionPerformed(CallEvent.CallAction.ACCEPT)
        binding.buttonAnswerCall.visibility = View.GONE
        binding.buttonHangUp.visibility = View.VISIBLE
    }

    private fun refuseCall() {
        callHandler.onActionPerformed(CallEvent.CallAction.REFUSE)
        releaseSurfaceRenderer()
        finish()
    }

    private fun hangUpCall() {
        callHandler.onActionPerformed(CallEvent.CallAction.HANG_UP)
        releaseSurfaceRenderer()
        finish()
    }

    private fun switchCamera() {
        service?.switchCamera()
    }

    private fun releaseSurfaceRenderer() {
        binding.localRenderer.release()
        binding.remoteRenderer.release()
    }

    private fun showAlertDialog() =
        AlertDialog.Builder(this)
            .setTitle(R.string.alert)
            .setMessage(R.string.error_failed_to_connect)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { hangUpCall() }
            .show()


    // motionlayout transition listener
    private val transitionListener = object : MotionLayout.TransitionListener {
        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
//            Timber.e("onTransitionTrigger")
        }

        override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
//            Timber.e("onTransitionStarted")
        }

        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
//            Timber.e("onTransitionChange")
        }

        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            if (currentId == R.id.end) {
                acceptCall()
            }
        }
    }
}