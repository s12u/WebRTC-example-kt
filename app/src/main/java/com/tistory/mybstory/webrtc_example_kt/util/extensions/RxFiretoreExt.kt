package com.tistory.mybstory.webrtc_example_kt.util.extensions

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.*
import com.tistory.mybstory.webrtc_example_kt.data.model.SyncEvent
import io.reactivex.*

inline fun <reified T : Any> DocumentReference.snapshotEvents(): Flowable<T> =
    Flowable.create({ emitter ->
        val snapshotListener = EventListener<DocumentSnapshot> { documentSnapshot, e ->
            documentSnapshot?.toObject(T::class.java)?.let {
                //TODO : null 문제 수정해야 함
                emitter.onNext(it)
            }
        }
        addSnapshotListener(snapshotListener)
    }, BackpressureStrategy.BUFFER)

inline fun <reified T : Any> CollectionReference.snapshotEvents(): Flowable<SyncEvent<T>> =
    Flowable.create({ emitter ->
        val snapshotListener = EventListener<QuerySnapshot> { querySnapshot, e ->
            querySnapshot?.let {
                for (documentChange in it.documentChanges) {
                    emitter.onNext(SyncEvent(documentChange.document.toObject(T::class.java), documentChange.type))
                }
            }
            e?.let {
                emitter.onError(e)
            }
        }
        addSnapshotListener(snapshotListener)
    }, BackpressureStrategy.BUFFER)

inline fun <reified T : Any> CollectionReference.allDocuments(): Single<List<T>> {
    return Flowable.create({ emitter: FlowableEmitter<List<T>> ->
        val snapshotListener = OnCompleteListener<QuerySnapshot> { task ->
            if (task.isSuccessful) {
                task.result?.apply {
                    emitter.onNext(toObjects(T::class.java))
                    emitter.onComplete()
                }
            }
        }
        get().addOnCompleteListener(snapshotListener)
    }, BackpressureStrategy.BUFFER).firstOrError()
}
