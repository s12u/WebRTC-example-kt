package com.tistory.mybstory.webrtc_example_kt.webrtc

import android.content.Context
import com.tistory.mybstory.webrtc_example_kt.base.PeerConnectionHandler
import com.tistory.mybstory.webrtc_example_kt.base.RemoteVideoHandler
import com.tistory.mybstory.webrtc_example_kt.util.RtcUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import timber.log.Timber

class RtcClient constructor(context: Context) : RemoteVideoHandler {

    private val eglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var surfaceTextureHelper: SurfaceTextureHelper
    private var videoCapturer: CameraVideoCapturer?

    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var remoteVideoTrack: VideoTrack? = null

    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null

    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    init {
        Timber.e("RtcClient instance created!!")
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

        surfaceTextureHelper = SurfaceTextureHelper.create("CapturingThread", eglBase.eglBaseContext)
        videoCapturer = RtcUtil.createVideoCapturer(context)

        videoCapturer?.let {
            localVideoSource = peerConnectionFactory.createVideoSource(false)
            localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", localVideoSource)
            it.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
        }

        localAudioSource = peerConnectionFactory.createAudioSource(mediaConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", localAudioSource)

    }

    fun initPeerConnection(
        iceServers: List<IceServer>,
        peerConnectionHandler: PeerConnectionHandler
    ) {

        val rtcConfiguration = PeerConnection.RTCConfiguration(iceServers)
        rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfiguration.enableDtlsSrtp = true

        val peerConnectionObserver = RtcPeerConnectionObserver(peerConnectionHandler, this)
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, peerConnectionObserver)

        val localMediaStream = peerConnectionFactory.createLocalMediaStream("ARDAMS")

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

    // TODO : need to run on new thread

    fun createOffer(sdpObserver: SimpleSdpObserver) {
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.createOffer(sdpObserver, mediaConstraints)
        }
    }

    fun createAnswer(sdpObserver: SimpleSdpObserver) {
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.createAnswer(sdpObserver, mediaConstraints)
        }
    }

    fun setLocalDescription(sdpObserver: SimpleSdpObserver, localDescription: SessionDescription) {
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.setLocalDescription(sdpObserver, localDescription)
        }
    }

    fun setRemoteDescription(sdpObserver: SimpleSdpObserver, remoteDescription: SessionDescription) {
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.setRemoteDescription(sdpObserver, remoteDescription)
        }
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.addIceCandidate(iceCandidate)
        }
    }

    fun removeIceCandidates(iceCandidates: Array<IceCandidate>) {
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.removeIceCandidates(iceCandidates)
        }
    }


    fun attachLocalView(localSurfaceViewRenderer: SurfaceViewRenderer) {
        localSurfaceViewRenderer.init(eglBase.eglBaseContext, null)
        this.localSurfaceViewRenderer = localSurfaceViewRenderer
        localVideoTrack?.addSink(this@RtcClient.localSurfaceViewRenderer)
        videoCapturer?.let { enableVideo(true, it) }
    }

    fun attachRemoteView(remoteSurfaceViewRenderer: SurfaceViewRenderer) {
        remoteSurfaceViewRenderer.init(eglBase.eglBaseContext, null)
        this.remoteSurfaceViewRenderer = remoteSurfaceViewRenderer
        remoteVideoTrack?.addSink(this@RtcClient.remoteSurfaceViewRenderer)
    }

    override fun onAddRemoteStream(remoteVideoTrack: VideoTrack) {
        this.remoteVideoTrack = remoteVideoTrack
        remoteSurfaceViewRenderer?.let {
            remoteVideoTrack.addSink(it)
        }
    }

    override fun removeVideoStream() {
        this.remoteVideoTrack = null
    }

    private fun enableVideo(isEnabled: Boolean, videoCapturer: VideoCapturer) {
        if (isEnabled) {
            videoCapturer.startCapture(1280, 720, 30)
        } else {
            videoCapturer.stopCapture()
        }
    }

    // TODO: error on switching camera
    fun switchCamera(cameraSwitchHandler: CameraVideoCapturer.CameraSwitchHandler? = null) =
        videoCapturer?.switchCamera(cameraSwitchHandler)

    fun detachViews() {
        localSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.release()
        localSurfaceViewRenderer = null
        remoteSurfaceViewRenderer = null
    }

    fun close() {
        detachViews()
        CoroutineScope(Dispatchers.IO).launch {
            peerConnection?.run {
                dispose()
            }
            videoCapturer?.dispose()
            localAudioSource?.dispose()
            localVideoSource?.dispose()
            surfaceTextureHelper.dispose()
            eglBase.release()
            peerConnectionFactory.dispose()
        }
    }


}