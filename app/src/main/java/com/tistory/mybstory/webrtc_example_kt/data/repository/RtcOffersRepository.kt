package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.base.Exceptions
import com.tistory.mybstory.webrtc_example_kt.data.model.FirestoreSessionDescription
import com.tistory.mybstory.webrtc_example_kt.service.AuthManager
import com.tistory.mybstory.webrtc_example_kt.util.extensions.await
import com.tistory.mybstory.webrtc_example_kt.util.extensions.snapshotEvents
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.webrtc.SessionDescription

class RtcOffersRepository private constructor() {

    val OFFER_PATH = "offers/"
    val firestore by lazy { FirebaseFirestore.getInstance() }

    // TODO : handle busy status
    fun create(recipientId: String, localSessionDescription: SessionDescription) = Completable.create { emitter ->
        firestore.collection(OFFER_PATH)
            .document(recipientId)
            .get().addOnSuccessListener { documentSnapshot->
                // busy
                if (documentSnapshot.exists()) {
                    emitter.onError(Exceptions.PeerIsBusyException())
                } else {
                    documentSnapshot.reference.set(FirestoreSessionDescription.fromSessionDescription(localSessionDescription)
                        .apply { senderId = AuthManager.getInstance().getUser()!!.uid })
                        .addOnSuccessListener {
                            removeOffer = removeSelf(recipientId) // remove created offer later -> TODO: considering db rules
                            emitter.onComplete()
                        }

                }
            }.addOnFailureListener { e ->
                emitter.onError(e)
            }
    }

    fun listenOffer(currentUid: String): Flowable<Pair<String, SessionDescription>> =
        firestore.collection(OFFER_PATH)
            .document(currentUid)
            .snapshotEvents<FirestoreSessionDescription>()
            .map { it.senderId to it.toSessionDescription() }
            .subscribeOn(Schedulers.io())

    private fun removeSelf(recipientId: String): Completable = Completable.create { emitter ->
        firestore.collection(OFFER_PATH)
            .document(recipientId)
            .delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    removeOffer = removeSelf(recipientId)
                    emitter.onComplete()
                }
                task.exception?.let { e ->
                    emitter.onError(e)
                }
            }
    }

    var removeOffer: Completable? = removeSelf(AuthManager.getInstance().getUser()!!.uid)

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