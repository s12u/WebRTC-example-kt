package com.tistory.mybstory.webrtc_example_kt.data.model

import org.webrtc.PeerConnection

data class CallEvent (val type: Type, val iceConnectionState: PeerConnection.IceConnectionState? , val action: CallAction?) {

    constructor(type: Type, state: PeerConnection.IceConnectionState) : this(type, state, null)
    constructor(type: Type, action: CallAction) : this(type, null, action)

    enum class Type {
        STATE_CHANGED, ACTION
    }

    enum class CallAction {
        ACCEPT, HANG_UP
    }
}