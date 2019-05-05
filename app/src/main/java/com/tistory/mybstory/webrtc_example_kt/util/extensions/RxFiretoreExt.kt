package com.tistory.mybstory.webrtc_example_kt.util.extensions

import com.google.firebase.firestore.*
import com.tistory.mybstory.webrtc_example_kt.data.model.SyncEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe

inline fun <reified T : Any> DocumentReference.snapshotEvents() = Flowable.create(FlowableOnSubscribe<T> { emitter ->
    val snapshotListener = EventListener<DocumentSnapshot> { documentSnapshot, e ->
        documentSnapshot?.let {
            emitter.onNext(documentSnapshot.toObject(T::class.java)!!)
        }
    }
    addSnapshotListener(snapshotListener)
}, BackpressureStrategy.BUFFER)

inline fun <reified T : Any> CollectionReference.snapshotEvents(): Flowable<SyncEvent<T>> =
    Flowable.create({ emitter ->
        val snapshotListsner = EventListener<QuerySnapshot> { querySnapshot, e ->
            querySnapshot?.let {
                for (documentChange in it.documentChanges) {
                    emitter.onNext(SyncEvent(documentChange.document.toObject(T::class.java), documentChange.type))
                }
            }
        }

        addSnapshotListener(snapshotListsner)
    }, BackpressureStrategy.BUFFER)