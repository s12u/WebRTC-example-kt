package com.tistory.mybstory.webrtc_example_kt.service

import android.os.Bundle
import com.google.firebase.firestore.DocumentChange
import com.tistory.mybstory.webrtc_example_kt.base.PeerConnectionHandler
import com.tistory.mybstory.webrtc_example_kt.data.model.CallEvent
import com.tistory.mybstory.webrtc_example_kt.data.model.CallEvent.CallAction
import com.tistory.mybstory.webrtc_example_kt.data.repository.IceCandidatesRepository
import com.tistory.mybstory.webrtc_example_kt.data.repository.IceServersRepository
import com.tistory.mybstory.webrtc_example_kt.data.repository.RtcAnswersRepository
import com.tistory.mybstory.webrtc_example_kt.data.repository.RtcOffersRepository
import com.tistory.mybstory.webrtc_example_kt.ui.CallActivity
import com.tistory.mybstory.webrtc_example_kt.util.extensions.launchActivity
import com.tistory.mybstory.webrtc_example_kt.webrtc.RtcClient
import com.tistory.mybstory.webrtc_example_kt.webrtc.SimpleSdpObserver
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber
import java.util.concurrent.TimeoutException

class RtcServiceController {

    private var rtcClient: RtcClient? = null
    private val callHandler by lazy { CallHandler.getInstance() }
    private val iceServersRepository by lazy { IceServersRepository.getInstance() }
    private val offersRepository by lazy { RtcOffersRepository.getInstance() }
    private val answersRepository by lazy { RtcAnswersRepository.getInstance() }
    private val iceCandidatesRepository by lazy { IceCandidatesRepository.getInstance() }

    private var disposables = CompositeDisposable()

    private var rtcService: RtcService? = null

    //TODO: need to make callHandler for caller/callee

    fun attachService(service: RtcService) {
        Timber.e("Service attached!")
        callHandler.create()
        rtcService = service
        getIceServers()
    }

    fun detachService() {
        Timber.e("Service detached!!")
        callHandler.dispose()
        closeRtcClient()
        rtcService = null
    }

    fun resetRtcClient() {
        closeRtcClient()
        getIceServers()
    }

    fun closeRtcClient() {
        CoroutineScope(Dispatchers.IO).launch {
            if (rtcClient != null) {
                disposables.clear()
                rtcClient?.close()
                rtcClient = null
            }
        }

    }

    fun getIceServers() {
        Timber.e("Ice servers loading...")
        disposables += iceServersRepository.getIceServers()
            .subscribe({
                Timber.e("Ice server count : %d", it.size)
                rtcClient = RtcClient(rtcService!!.applicationContext)
                initRtcClient(it, peerConnectionHandler)
                listenForOffer(AuthManager.getInstance().getUser()!!.uid)
            }, {
                // error handling
            })

    }

    fun initRtcClient(iceServers: List<PeerConnection.IceServer>, peerConnectionHandler: PeerConnectionHandler) =
        rtcClient?.initPeerConnection(iceServers, peerConnectionHandler)


    fun attachLocalView(localRender: SurfaceViewRenderer) = rtcClient?.attachLocalView(localRender)

    fun attachRemoteView(remoteRender: SurfaceViewRenderer) = rtcClient?.attachRemoteView(remoteRender)

    fun switchCamera() = rtcClient?.switchCamera()

    /**
     *
     * for caller
     *
     **/

    //TODO: need to delete offer/answer/ice candidates/ at connection closed

    //TODO: need to implement clear offer on disconnect (caller) & detect "disconnected" event
    fun offerDevice(remoteUid: String) {
        Timber.e("Listening for ICE Candidates...(Caller)")
        listenForIceCandidates(remoteUid)
        creteOffer(remoteUid)
    }

    fun creteOffer(remoteUid: String) {
        Timber.e("Creating offer....")
        rtcClient?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                p0?.let {
                    Timber.e("Local description for offer created!")
                    setLocalOfferDescription(it, remoteUid)
                }
            }

            override fun onCreateFailure(p0: String?) {
                super.onCreateFailure(p0)
                Timber.e("failed to create Local description")
            }
        })
    }


    fun setLocalOfferDescription(localDescription: SessionDescription, remoteUid: String) =
        rtcClient?.setLocalDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                Timber.e("Local description set!!, Sending offer!!")
                sendOffer(remoteUid, localDescription)
            }

            override fun onSetFailure(p0: String?) {
                super.onSetFailure(p0)
                Timber.e("failed to set Local offer description")
            }
        }, localDescription)


    fun sendOffer(remoteUid: String, localDescription: SessionDescription) {
        disposables += offersRepository.create(remoteUid, localDescription)
            //.timeout(20, TimeUnit.SECONDS)
            .subscribe(
                {
                    // success
                    Timber.e("Offer sent!!, listening for answer!!")
                    listenForAnswer()
                },
                {
                    if (it is TimeoutException) {
                        //TODO: timeout.....remove offer from db
                        //callHandler.onActionPerformed(CallAction.HANG_UP)
                    }
                    // error
                })
    }

    fun listenForAnswer() {
        disposables += answersRepository
            .listenAnswer()
            .doOnNext {
                Timber.e("Answer received !! ")
                callHandler.onActionPerformed(CallEvent.CallAction.READY)
            }
            .combineLatest(callHandler.callback.toFlowable(BackpressureStrategy.LATEST))
            .subscribe {
                handleCallEvent(it.first, it.second)
            }
    }

    fun setRemoteOfferDescription(remoteDescription: SessionDescription) =
        rtcClient?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                super.onSetSuccess()
                Timber.e("Remote session description for offer set!!")
            }

            override fun onSetFailure(p0: String?) {
                Timber.e("failed to set remote offer description")
                super.onSetFailure(p0)
            }
        }, remoteDescription)


    fun listenForIceCandidates(remoteUid: String) {
        disposables += iceCandidatesRepository.get(remoteUid)
            .subscribe {
                when (it.type) {
                    DocumentChange.Type.ADDED -> {
                        rtcClient?.addIceCandidate(it.value.toIceCandidate())
                        Timber.e("Ice candidate added!!")
                    }
                    DocumentChange.Type.REMOVED -> {
                        rtcClient?.removeIceCandidates(arrayOf(it.value.toIceCandidate()))
                        Timber.e("Ice candidate removed!!")
                    }
                    else -> {
                        Timber.e("unknown type of operation")
                    }
                }
            }
    }

    fun sendIceCandidate(iceCandidate: IceCandidate) {
        disposables += iceCandidatesRepository.send(iceCandidate)
            .subscribe {
                Timber.e("Ice candidate sent")
            }

    }

    fun removeIceCandidates(iceCandidates: Array<IceCandidate>) {
        disposables += iceCandidatesRepository.remove(iceCandidates)
            .subscribe {
                Timber.e("Ice candidates removed from remote DB")
            }
    }

    /**
     *
     * for callee
     *
     **/


    // TODO: action에 따라서 분기 시켜야 함
    fun listenForOffer(currentUid: String) {
        Timber.e("Wating for offer...")
        disposables += offersRepository
            .listenOffer(currentUid)
            .doOnNext {
                Timber.e("Offer received!!")
                startCallActivity(it.first)
            }.combineLatest( // TODO: need to implement on caller-logic
                callHandler.callback.toFlowable(BackpressureStrategy.LATEST)
            ).subscribe({
                //TODO: need to refactoring
                Timber.e("Listening for ICE Candidates..(Callee)")
                handleCallEvent(it.first, it.second)
            }, {
                // error
            })
    }

    fun setRemoteAnswerDescription(remoteUid: String, remoteDescription: SessionDescription) =
        rtcClient?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                super.onSetSuccess()
                Timber.e("Remote session description for answer set!!")
                createAnswer(remoteUid)
            }

            override fun onSetFailure(p0: String?) {
                super.onSetFailure(p0)
            }
        }, remoteDescription)


    fun createAnswer(remoteUid: String) =
        rtcClient?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                p0?.let {
                    Timber.e("Local description for answer created!")
                    setLocalAnswerDescription(remoteUid, p0)
                }
            }

            override fun onCreateFailure(p0: String?) {
                super.onCreateFailure(p0)
                Timber.e("failed to create Local description")
            }
        })


    fun setLocalAnswerDescription(remoteUid: String, localDescription: SessionDescription) =
        rtcClient?.setLocalDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                Timber.e("Local description set!!, Send Answer!!")
                sendAnswer(remoteUid, localDescription)
            }

            override fun onSetFailure(p0: String?) {
                Timber.e("failed to set local answer description")
                super.onSetFailure(p0)
            }
        }, localDescription)


    fun sendAnswer(remoteUid: String, localDescription: SessionDescription) {
        disposables += answersRepository
            .create(remoteUid, localDescription)
            .subscribe {
                Timber.e("Answer sent to $remoteUid!!")
            }

    }

    val peerConnectionHandler: PeerConnectionHandler = object : PeerConnectionHandler {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            sendIceCandidate(iceCandidate)
            Timber.e("onIceCandidate() called")
        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
            removeIceCandidates(iceCandidates)
            Timber.e("onIceCandidatesRemoved() called")
        }

        override fun onIceConnectionChanged(iceConnectionState: PeerConnection.IceConnectionState) {
            Timber.e("ICE Connection state changed : %s", iceConnectionState.name)
            callHandler.onConnectionStateChanged(iceConnectionState)
            handleStateChanges(iceConnectionState)
        }
    }

    fun handleCallEvent(sdp: Pair<String, SessionDescription>, callEvent: CallEvent) {
        when (callEvent.type) {
            CallEvent.Type.STATE_CHANGED -> {

            }
            CallEvent.Type.ACTION -> {
                Timber.e("Action ${callEvent.action} performed!")
                handleCallAction(sdp, callEvent.action!!)
            }
        }
    }

    fun handleStateChanges(
        iceConnectionState: PeerConnection.IceConnectionState
    ) {
        when (iceConnectionState) {
            PeerConnection.IceConnectionState.CLOSED,
            PeerConnection.IceConnectionState.DISCONNECTED,
            PeerConnection.IceConnectionState.FAILED -> {
                removeOfferAndAnswer()
                //resetRtcClient()
            }

            else -> {

            }
        }
    }

    fun handleCallAction(
        sdp: Pair<String, SessionDescription>? = null,
        callAction: CallAction
    ) = when (callAction) {
        CallAction.READY -> {
            setRemoteOfferDescription(sdp!!.second)
        }
        CallAction.ACCEPT -> {
            listenForIceCandidates(sdp!!.first)
            setRemoteAnswerDescription(sdp.first, sdp.second)
        }
        CallAction.HANG_UP -> {
            resetRtcClient()
        }
        CallAction.REFUSE -> {
            resetRtcClient()
        }
    }

    private fun removeOfferAndAnswer() {
        offersRepository.removeOffer?.subscribe()
        answersRepository.removeAnswer?.subscribe()
    }

    private fun startCallActivity(remoteUid: String) {
        val bundle = Bundle()
        bundle.putString("remoteUID", remoteUid)
        bundle.putBoolean("isCaller", false)
        rtcService!!.launchActivity<CallActivity>(bundle)
    }

}