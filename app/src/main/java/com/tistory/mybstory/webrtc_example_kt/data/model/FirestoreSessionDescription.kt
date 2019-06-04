package com.tistory.mybstory.webrtc_example_kt.data.model

import org.webrtc.SessionDescription

data class FirestoreSessionDescription(
    var senderId: String = "",
    var type: SessionDescription.Type = SessionDescription.Type.OFFER,
    var description: String = ""
) {
    companion object {
        fun fromSessionDescription(sessionDescription: SessionDescription) = with(sessionDescription) {
            FirestoreSessionDescription("", type, description)
        }
    }

    fun toSessionDescription(): SessionDescription = SessionDescription(type, description)

}