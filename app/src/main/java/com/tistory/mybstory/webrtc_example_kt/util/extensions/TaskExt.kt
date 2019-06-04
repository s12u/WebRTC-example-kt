package com.tistory.mybstory.webrtc_example_kt.util.extensions

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> Task<T>.await() : T? =
    suspendCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result!!)
            } else {
                continuation.resumeWithException(task.exception!!)
            }
        }
    }