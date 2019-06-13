package com.tistory.mybstory.webrtc_example_kt.service

import com.tistory.mybstory.webrtc_example_kt.data.model.CallEvent
import io.reactivex.subjects.PublishSubject
import org.webrtc.PeerConnection.IceConnectionState

class CallHandler {

    companion object {
        private var instance: CallHandler? = null

        fun getInstance(): CallHandler {
            if (instance == null) {
                instance = CallHandler()
            }
            return instance!!
        }
    }

    var callback = create()

    fun create() = PublishSubject.create<CallEvent>()

    fun dispose() = callback.onComplete()

    fun onConnectionStateChanged(state: IceConnectionState) {
        callback.onNext(CallEvent(CallEvent.Type.STATE_CHANGED, state))
    }

    fun onActionPerformed(action: CallEvent.CallAction) {
        callback.onNext(CallEvent(CallEvent.Type.ACTION, action))
    }

}