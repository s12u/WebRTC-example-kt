package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.data.model.FirestoreSessionDescription
import com.tistory.mybstory.webrtc_example_kt.service.AuthManager
import com.tistory.mybstory.webrtc_example_kt.util.extensions.snapshotEvents
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.webrtc.SessionDescription

class RtcOffersRepository private constructor() {

    val OFFER_PATH = "offers/"
    val firestore by lazy { FirebaseFirestore.getInstance() }

    fun create(recipientId: String, localSessionDescription: SessionDescription) = Completable.create {
        firestore.collection(OFFER_PATH)
            .document(recipientId)
            // TODO: need to refactor AuthManager uid fetch method
            .set(FirestoreSessionDescription.fromSessionDescription(localSessionDescription).apply { senderId = AuthManager.getInstance().getUser()!!.uid })
        it.onComplete()
    }

    fun listenOffer(currentUid : String): Flowable<Pair<String, SessionDescription>> =
        firestore.collection(OFFER_PATH)
            .document(currentUid)
            .snapshotEvents<FirestoreSessionDescription>()
            .map { Pair(it.senderId, it.toSessionDescription()) }
            .subscribeOn(Schedulers.io())

    companion object {
        private var instance: RtcOffersRepository? = null

        fun getInstance(): RtcOffersRepository {
            if (instance == null) {
                instance = RtcOffersRepository()
            }
            return instance!!
        }
    }
}