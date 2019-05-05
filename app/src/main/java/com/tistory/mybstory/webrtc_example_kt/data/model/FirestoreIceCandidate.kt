package com.tistory.mybstory.webrtc_example_kt.data.model

import org.webrtc.IceCandidate

data class FirestoreIceCandidate(var sdpMLineMax: Int, var sdpMid: String, var sdp: String) {
    companion object {
        fun fromIceCandidate(iceCandidate: IceCandidate) = with(iceCandidate) {
            FirestoreIceCandidate(sdpMLineIndex, sdpMid, sdp)
        }
    }
}

