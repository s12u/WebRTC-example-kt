package com.tistory.mybstory.webrtc_example_kt.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tistory.mybstory.webrtc_example_kt.util.extensions.snapshotEvents
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.webrtc.PeerConnection

class IceServersRepository private constructor() {

    val ICE_SERVER_PATH = "ice_servers/"
    val firestore by lazy { FirebaseFirestore.getInstance() }

    fun getIceServers(): Single<List<PeerConnection.IceServer>> =
        firestore.collection(ICE_SERVER_PATH)
            .snapshotEvents<PeerConnection.IceServer>()
            .map { it.value }
            .toList()
            .subscribeOn(Schedulers.io())

    companion object {
        private var instance: IceServersRepository? = null

        fun getInstance(): IceServersRepository {
            if (instance == null) {
                instance = IceServersRepository()
            }
            return instance!!
        }
    }
}