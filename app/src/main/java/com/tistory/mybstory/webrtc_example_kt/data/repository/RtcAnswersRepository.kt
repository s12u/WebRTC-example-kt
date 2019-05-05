package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.data.model.FirestoreSessionDescription
import com.tistory.mybstory.webrtc_example_kt.util.extensions.snapshotEvents
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.webrtc.SessionDescription

class RtcAnswersRepository private constructor() {

    val ANSWER_PATH = "answers/"
    val firestore by lazy { FirebaseFirestore.getInstance() }

    fun create(recipientId: String, localSessionDescription: SessionDescription) = Completable.create {
        firestore.collection(ANSWER_PATH)
            .document(recipientId)
            .set(FirestoreSessionDescription.fromSessionDescription(localSessionDescription))
        it.onComplete()
    }

    fun listenAnswer(): Flowable<SessionDescription> =
        firestore.collection(ANSWER_PATH)
            .document("CURRENT_UID")
            .snapshotEvents<FirestoreSessionDescription>()
            .map { it.toSessionDescription() }
            .subscribeOn(Schedulers.io())

    companion object {
        private var instance: RtcAnswersRepository? = null

        fun getInstance(): RtcAnswersRepository {
            if (instance == null) {
                instance = RtcAnswersRepository()
            }
            return instance!!
        }
    }
}