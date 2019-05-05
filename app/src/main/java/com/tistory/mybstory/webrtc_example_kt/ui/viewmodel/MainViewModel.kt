package com.tistory.mybstory.webrtc_example_kt.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.tistory.mybstory.webrtc_example_kt.service.AuthManager

class MainViewModel: ViewModel(){

    private val userLiveData = MutableLiveData<FirebaseUser>()

    private val onCompleteListener = OnCompleteListener<AuthResult> {
        if (it.isSuccessful) {
            userLiveData.value = it.result?.user
        } else {
            Log.e("MainViewModel", "Login failed!")
        }
    }

    init {
        AuthManager.getInstance().signIn(onCompleteListener)
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun observeUser(): LiveData<FirebaseUser> = userLiveData

}