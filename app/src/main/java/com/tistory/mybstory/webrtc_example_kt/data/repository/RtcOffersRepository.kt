package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.data.model.FirestoreSessionDescription
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
            .set(FirestoreSessionDescription.fromSessionDescription(localSessionDescription))
        it.onComplete()
    }

    fun listenOffer(): Flowable<Pair<SessionDescription, String>> =
        firestore.collection(OFFER_PATH)
            .document("CURRENT_UID")
            .snapshotEvents<FirestoreSessionDescription>()
            .map { Pair(it.toSessionDescription(), it.senderId) }
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