package com.tistory.mybstory.webrtc_example_kt.service

import android.content.Context
import com.tistory.mybstory.webrtc_example_kt.webrtc.RtcClient

class RtcServiceController constructor(context: Context){

    var rtcClient = RtcClient.getInstance(context)

    lateinit var rtcService: RtcService

    fun attachService(service: RtcService) {
        rtcService = service
    }

    fun detachService() {
        rtcClient// need to clear & detach
    }


}