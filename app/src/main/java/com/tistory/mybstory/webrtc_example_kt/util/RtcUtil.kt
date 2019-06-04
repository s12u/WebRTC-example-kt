package com.tistory.mybstory.webrtc_example_kt.util

import android.content.Context
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.VideoCapturer

class RtcUtil {

    companion object {

        fun createVideoCapturer(context: Context) : VideoCapturer? {

            val enumerator = if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator()
            val deviceNames = enumerator.deviceNames

            // front camera
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    return enumerator.createCapturer(deviceName, null)
                }
            }

            // other camera
            for (deviceName in deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    return enumerator.createCapturer(deviceName, null)
                }
            }

            // failed to get camera
            return null
        }
    }

}