package com.tistory.mybstory.webrtc_example_kt.data.model

import org.webrtc.IceCandidate

data class FirestoreIceCandidate(var sdpMid: String = "", var sdpMLineMax: Int = 0,  var sdp: String = "") {
    companion object {
        fun fromIceCandidate(iceCandidate: IceCandidate) = with(iceCandidate) {
            FirestoreIceCandidate(sdpMid, sdpMLineIndex, sdp)
        }
    }

    fun toIceCandidate() = IceCandidate(sdpMid, sdpMLineMax, sdp)
}

