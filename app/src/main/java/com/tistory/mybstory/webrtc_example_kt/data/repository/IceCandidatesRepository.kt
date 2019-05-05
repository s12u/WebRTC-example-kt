package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.data.model.FirestoreIceCandidate
import com.tistory.mybstory.webrtc_example_kt.data.model.SyncEvent
import com.tistory.mybstory.webrtc_example_kt.util.extensions.snapshotEvents
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.webrtc.IceCandidate

class IceCandidatesRepository private constructor() {

    val ICE_CANDIDATES_PATH = "ice_candidates/"

    val firestore by lazy { FirebaseFirestore.getInstance() }

    fun send(iceCandidate: IceCandidate): Completable =
        Completable.create {
            val iceReference = firestore.collection(ICE_CANDIDATES_PATH)
                .document("CURRENT_UID")
                .collection(ICE_CANDIDATES_PATH)
            val newIce = iceReference.document()

            iceReference.document(newIce.id)
                .set(FirestoreIceCandidate.fromIceCandidate(iceCandidate))
            it.onComplete()
        }


    fun get(remoteUid: String): Flowable<SyncEvent<FirestoreIceCandidate>> =
            firestore.collection(ICE_CANDIDATES_PATH)
                .document(remoteUid)
                .collection(ICE_CANDIDATES_PATH)
                .snapshotEvents<FirestoreIceCandidate>()
                .filter{ it.type == DocumentChange.Type.ADDED || it.type == DocumentChange.Type.REMOVED }
                .subscribeOn(Schedulers.io())

    companion object {
        private var instance: IceCandidatesRepository? = null

        fun getInstance(): IceCandidatesRepository {
            if (instance == null) {
                instance = IceCandidatesRepository()
            }
            return instance!!
        }
    }
}