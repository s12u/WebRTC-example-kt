package com.tistory.mybstory.webrtc_example_kt.service

import com.tistory.mybstory.webrtc_example_kt.data.model.CallEvent
import io.reactivex.subjects.PublishSubject
import org.webrtc.PeerConnection.IceConnectionState

class CallHandler {

    var callback: PublishSubject<CallEvent> = PublishSubject.create()

    companion object {
        private var instance: CallHandler? = null

        fun getInstance(): CallHandler {
            if (instance == null) {
                instance = CallHandler()
            }
            return instance!!
        }
    }

    fun create() { callback  = PublishSubject.create()}

    fun dispose() = callback.onComplete()

    fun onConnectionStateChanged(state: IceConnectionState) {
        callback.onNext(CallEvent(CallEvent.Type.STATE_CHANGED, state))
    }

    fun onActionPerformed(action: CallEvent.CallAction) {
        callback.onNext(CallEvent(CallEvent.Type.ACTION, action))
    }

    fun onError(error: Throwable) {
        callback.onError(error)
    }
}