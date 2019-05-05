package com.tistory.mybstory.webrtc_example_kt.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KClass

open class BaseActivity : AppCompatActivity() {

    inline fun <reified T : Any> Context.launchActivity(
        options: Bundle? = null) {
        startActivity(newIntent<T>(this).apply {
            options?.let{putExtras(options)}
        })
    }

    inline fun <reified T : Any> newIntent(context: Context): Intent =
        Intent(context, T::class.java)
}