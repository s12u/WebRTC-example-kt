package com.tistory.mybstory.webrtc_example_kt.data.model

import com.google.firebase.firestore.DocumentChange

data class SyncEvent<T> (var value:T, var type:DocumentChange.Type)