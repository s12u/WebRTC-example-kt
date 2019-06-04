package com.tistory.mybstory.webrtc_example_kt.base

import org.webrtc.VideoTrack

interface RemoteVideoHandler {
    fun onAddRemoteStream(remoteVideoTrack: VideoTrack)
    fun removeVideoStream()
}