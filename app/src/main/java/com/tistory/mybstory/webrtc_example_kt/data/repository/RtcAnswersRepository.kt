package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.android.gms.common.api.Result
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.data.model.FirestoreSessionDescription
import com.tistory.mybstory.webrtc_example_kt.service.AuthManager
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
            .set(FirestoreSessionDescription.fromSessionDescription(localSessionDescription)
                .apply { senderId = AuthManager.getInstance().getUser()!!.uid })
            removeAnswer = removeSelf(recipientId) // remove created answer later
        it.onComplete()
    }

    private fun removeSelf(recipientId: String) = Completable.create { emitter ->
        firestore.collection(ANSWER_PATH)
            .document(recipientId)
            .delete()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    removeAnswer = null
                    emitter.onComplete()
                }
                it.exception?.let { e ->
                    emitter.onError(e)
                }
            }
    }

    fun listenAnswer(): Flowable<SessionDescription> =
        firestore.collection(ANSWER_PATH)
            .document(AuthManager.getInstance().getUser()!!.uid)
            .snapshotEvents<FirestoreSessionDescription>()
            .map { it.toSessionDescription() }
            .subscribeOn(Schedulers.io())


    var removeAnswer: Completable? = null

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