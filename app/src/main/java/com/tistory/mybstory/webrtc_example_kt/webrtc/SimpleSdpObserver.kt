package com.tistory.mybstory.webrtc_example_kt.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

abstract class SimpleSdpObserver : SdpObserver {
    override fun onSetFailure(p0: String?) {
    }

    override fun onSetSuccess() {
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
    }

    override fun onCreateFailure(p0: String?) {
    }

}