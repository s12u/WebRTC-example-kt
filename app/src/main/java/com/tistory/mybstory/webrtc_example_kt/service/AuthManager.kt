package com.tistory.mybstory.webrtc_example_kt.service

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser

class AuthManager private constructor() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private var user: FirebaseUser? = null
    private var authStateListener: AuthStateListener

    init {
        authStateListener = AuthStateListener {
            user = it.currentUser
        }
    }

    fun subscribe() = auth.addAuthStateListener(authStateListener)

    fun unSubscribe() = auth.removeAuthStateListener(authStateListener)

    fun signOut() = auth.signOut()

    fun signIn(onCompleteListener: OnCompleteListener<AuthResult>) =
        auth.signInAnonymously()
            .addOnCompleteListener(onCompleteListener)

    fun getUser(): FirebaseUser? = auth.currentUser

    companion object {
        private var instance: AuthManager? = null
        fun getInstance(): AuthManager {
            if (instance == null) {
                instance = AuthManager()
            }
            return instance!!
        }
    }
}