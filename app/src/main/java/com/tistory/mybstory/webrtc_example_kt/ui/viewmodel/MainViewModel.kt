package com.tistory.mybstory.webrtc_example_kt.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.tistory.mybstory.webrtc_example_kt.service.AuthManager
import com.tistory.mybstory.webrtc_example_kt.util.extensions.await
import timber.log.Timber

class MainViewModel : ViewModel() {

    private val userLiveData = MutableLiveData<FirebaseUser>()

    private val onCompleteListener = OnCompleteListener<AuthResult> {
        if (it.isSuccessful) {
            userLiveData.value = it.result?.user
            Timber.e("current uid : ${it.result?.user?.uid}")
        } else {
            Log.e("MainViewModel", "Login failed!")
        }
    }

    init {

    }

    override fun onCleared() {
        super.onCleared()
    }

    suspend fun signIn() = AuthManager.getInstance()
            .signIn(onCompleteListener)

    fun observeUser(): LiveData<FirebaseUser> = userLiveData

}