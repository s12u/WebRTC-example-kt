package com.tistory.mybstory.webrtc_example_kt.base

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection

interface PeerConnectionHandler {
    fun onIceCandidate(iceCandidate: IceCandidate)
    fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>)
    fun onIceConnectionChanged(iceConnectionState: PeerConnection.IceConnectionState)
}