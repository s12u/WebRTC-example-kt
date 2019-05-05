package com.tistory.mybstory.webrtc_example_kt.ui

import android.app.Activity
import android.os.Bundle
import timber.log.Timber

class CallActivity : Activity() {

    private var remoteUID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.getStringExtra("remoteUID")?.let{
            remoteUID = it
            Timber.d("Remote uid : $remoteUID")
        }
    }


}