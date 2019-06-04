package com.tistory.mybstory.webrtc_example_kt.webrtc

import com.tistory.mybstory.webrtc_example_kt.base.PeerConnectionHandler
import com.tistory.mybstory.webrtc_example_kt.base.RemoteVideoHandler
import org.webrtc.*
import timber.log.Timber

class RtcPeerConnectionObserver(var peerConnectionHandler: PeerConnectionHandler, var remoteVideoHandler: RemoteVideoHandler) : PeerConnection.Observer {

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        peerConnectionHandler.onIceCandidate(iceCandidate)
    }

    override fun onDataChannel(dataChannel: DataChannel) {
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        peerConnectionHandler.onIceConnectionChanged(iceConnectionState)
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    override fun onAddStream(mediaStream: MediaStream) {
        if (mediaStream.videoTracks.size == 1) {
            Timber.e("Remote video stream found!!")
            val remoteVideoTrack = mediaStream.videoTracks[0]
            remoteVideoHandler.onAddRemoteStream(remoteVideoTrack)
        }
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {

    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        peerConnectionHandler.onIceCandidatesRemoved(iceCandidates)
    }

    override fun onRemoveStream(p0: MediaStream?) {
        remoteVideoHandler.removeVideoStream()
    }

    override fun onRenegotiationNeeded() {

    }

    override fun onAddTrack(p0: RtpReceiver?, mediaStreams: Array<out MediaStream>) {
        Timber.e("onAddTrack() called!!")

    }

}