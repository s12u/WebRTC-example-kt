package com.tistory.mybstory.webrtc_example_kt.data.model

import org.webrtc.PeerConnection

data class FirestoreIceServer(var uri: String = "", var username: String? = null, var password: String? = null) {

    fun toIceServer(): PeerConnection.IceServer = with(PeerConnection.IceServer.builder(uri)) {
        if (username == null || password == null) {
            createIceServer()
        } else {
            setUsername(username)
                .setPassword(password)
                .createIceServer()
        }
    }
}