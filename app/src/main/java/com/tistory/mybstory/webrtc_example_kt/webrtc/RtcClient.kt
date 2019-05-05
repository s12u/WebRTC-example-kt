package com.tistory.mybstory.webrtc_example_kt.webrtc

import android.content.Context
import com.tistory.mybstory.webrtc_example_kt.base.PeerConnectionListener
import com.tistory.mybstory.webrtc_example_kt.util.RtcUtil
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer

class RtcClient private constructor(context: Context) {

    private val eglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var peerConnectionListener: PeerConnectionListener? = null
    private var surfaceTextureHelper: SurfaceTextureHelper
    private var videoCapturer: VideoCapturer?

    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    companion object {
        private var instance: RtcClient? = null

        fun getInstance(context: Context): RtcClient {
            if (instance == null) {
                instance = RtcClient(context)
            }
            return instance!!
        }
    }

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        val mediaConstraints = MediaConstraints()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()

        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
        videoCapturer = RtcUtil.createVideoCapturer(context)

        videoCapturer?.let {
            localVideoSource = peerConnectionFactory.createVideoSource(false)
            localVideoTrack = peerConnectionFactory.createVideoTrack("VIDEO_01", localVideoSource)
            it.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
            //enableVideo(true, it)
        }

        localAudioSource = peerConnectionFactory.createAudioSource(mediaConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("AUDIO_01", localAudioSource)

    }

    private fun enableVideo(isEnabled: Boolean, videoCapturer: VideoCapturer) {
        if (isEnabled) {
            videoCapturer.startCapture(1920, 1080, 30)
        } else {
            videoCapturer.stopCapture()
        }
    }

    fun initPeerConnection(iceServers: List<IceServer>,
        peerConnectionListener: PeerConnectionListener,
        peerConnectionObserver: PeerConnection.Observer) {

        val rtcConfiguration = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, peerConnectionObserver)
        this.peerConnectionListener = peerConnectionListener
        val localMediaStream = peerConnectionFactory.createLocalMediaStream("localMediaStream")

        localMediaStream.apply {
            localAudioTrack?.let {
                addTrack(it)
            }
            localVideoTrack?.let {
                addTrack(it)
            }
        }
        peerConnection?.addStream(localMediaStream)
    }
}