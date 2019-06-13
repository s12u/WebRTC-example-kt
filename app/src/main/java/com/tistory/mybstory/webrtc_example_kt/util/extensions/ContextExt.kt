package com.tistory.mybstory.webrtc_example_kt.util.extensions

import android.content.Context
import android.content.Intent
import android.os.Bundle

inline fun <reified T : Any> Context.launchActivity(options: Bundle? = null) {
    startActivity(newIntent<T>(this).apply {
        options?.let { putExtras(options) }
    })
}

inline fun <reified T : Any> newIntent(context: Context): Intent =
    Intent(context, T::class.java)