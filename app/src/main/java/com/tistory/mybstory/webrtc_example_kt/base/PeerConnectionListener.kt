package com.tistory.mybstory.webrtc_example_kt.base

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection.IceConnectionState

interface PeerConnectionListener {
    fun onIceCandidate(iceCandidate: IceCandidate)
    fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>)
    fun onIceConnectionChanged(iceConnectionState: IceConnectionState)
}