package com.tistory.mybstory.webrtc_example_kt.service

import android.content.Context
import android.os.Bundle
import com.google.firebase.firestore.DocumentChange
import com.tistory.mybstory.webrtc_example_kt.base.PeerConnectionHandler
import com.tistory.mybstory.webrtc_example_kt.data.repository.IceCandidatesRepository
import com.tistory.mybstory.webrtc_example_kt.data.repository.IceServersRepository
import com.tistory.mybstory.webrtc_example_kt.data.repository.RtcAnswersRepository
import com.tistory.mybstory.webrtc_example_kt.data.repository.RtcOffersRepository
import com.tistory.mybstory.webrtc_example_kt.ui.CallActivity
import com.tistory.mybstory.webrtc_example_kt.util.extensions.launchActivity
import com.tistory.mybstory.webrtc_example_kt.webrtc.RtcClient
import com.tistory.mybstory.webrtc_example_kt.webrtc.SimpleSdpObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber

class RtcServiceController constructor(context: Context) {

    val rtcClient by lazy { RtcClient.getInstance(context) }
    val iceServersRepository by lazy { IceServersRepository.getInstance() }

    var disposables = CompositeDisposable()

    lateinit var rtcService: RtcService

    fun attachService(service: RtcService) {
        Timber.e("Service attached!")
        rtcService = service
        getIceServers()
    }

    fun detachService() {
        rtcClient// need to clear & detach
    }

    fun getIceServers() {
        Timber.e("Ice servers loading...")
        disposables += iceServersRepository.getIceServers()
            .subscribe({
                Timber.e("Ice server count : %d", it.size)
                initRtcClient(it, peerConnectionHandler)
                listenForOffer(AuthManager.getInstance().getUser()!!.uid)
            }, {
                // error handling
            })

    }

    fun initRtcClient(iceServers: List<PeerConnection.IceServer>, peerConnectionHandler: PeerConnectionHandler) =
        rtcClient.initPeerConnection(iceServers, peerConnectionHandler)


    fun attachLocalView(localRender: SurfaceViewRenderer) = rtcClient.attachLocalView(localRender)

    fun attachRemoteView(remoteRender: SurfaceViewRenderer) = rtcClient.attachRemoteView(remoteRender)

    /**
     *
     * for caller
     *
     **/

    fun offerDevice(remoteUid: String) = creteOffer(remoteUid)

    fun creteOffer(remoteUid: String) {
        Timber.d("Creating offer....")
        rtcClient.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                p0?.let {
                    Timber.e("Local description for offer created!")
                    setLocalOfferDescription(p0, remoteUid)
                }
            }

            override fun onCreateFailure(p0: String?) {
                super.onCreateFailure(p0)
                Timber.e("failed to create Local description")
            }
        })
    }


    fun setLocalOfferDescription(localDescription: SessionDescription, remoteUid: String) {
        rtcClient.setLocalDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                Timber.e("Local description set!!, Sending offer!!")
                sendOffer(remoteUid, localDescription)
            }

            override fun onSetFailure(p0: String?) {
                super.onSetFailure(p0)
            }
        }, localDescription)
    }

    fun sendOffer(remoteUid: String, localDescription: SessionDescription) {
        disposables += RtcOffersRepository.getInstance()
            .create(remoteUid, localDescription)
            .subscribe(
                {
                    // success
                    Timber.e("Offer sent!!, listening for answer!!")
                    listenForAnswer(remoteUid)
                },
                {
                    // error
                })
    }

    fun listenForAnswer(remoteUid: String) {
        disposables += RtcAnswersRepository.getInstance()
            .listenAnswer()
            .subscribe {
                setRemoteOfferDescription(remoteUid, it)
            }

    }

    fun setRemoteOfferDescription(remoteUid: String, remoteDescription: SessionDescription) {
        rtcClient.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetFailure(p0: String?) {
                super.onSetFailure(p0)
            }

            override fun onSetSuccess() {
                super.onSetSuccess()
                Timber.e("Remote session description for offer set!!")
                listenForIceCandidates(remoteUid)
            }
        }, remoteDescription)
    }

    fun listenForIceCandidates(remoteUid: String) {
        disposables +=
            IceCandidatesRepository.getInstance()
                .get(remoteUid).subscribe {
                    if (it.type == DocumentChange.Type.ADDED) {
                        rtcClient.addIceCandidate(it.value.toIceCandidate())
                        Timber.e("Ice candidate added!!")
                    } else {
                        rtcClient.removeIceCandidates(arrayOf(it.value.toIceCandidate()))
                        Timber.e("Ice candidate removed!!")
                    }
                }
    }

    fun sendIceCandidate(iceCandidate: IceCandidate) {
        disposables +=
            IceCandidatesRepository.getInstance()
                .send(iceCandidate)
                .subscribe {
                    Timber.e("Ice candidate sent")
                }

    }

    fun removeIceCandidates(iceCandidates: Array<IceCandidate>) {
        disposables += IceCandidatesRepository.getInstance()
            .remove(iceCandidates).subscribe {
                Timber.e("Ice candidates removed from remote DB")
            }
    }

    /**
     *
     * for callee
     *
     **/

    fun listenForOffer(currentUid: String) {
        Timber.e("Wating for offer...")
        disposables +=
            RtcOffersRepository.getInstance()
                .listenOffer(currentUid).subscribe({
                    startCallActivity(it.first)
                    setRemoteAnswerDescription(it.first, it.second)
                    Timber.e("Remote description set")
                }, {
                    // error
                })
    }

    fun setRemoteAnswerDescription(remoteUid: String, remoteDescription: SessionDescription) {
        rtcClient.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetFailure(p0: String?) {
                super.onSetFailure(p0)
            }

            override fun onSetSuccess() {
                super.onSetSuccess()
                Timber.e("Remote session description for answer set!!")
                createAnswer(remoteUid)
                listenForIceCandidates(remoteUid)
            }
        }, remoteDescription)
    }

    fun createAnswer(remoteUid: String) {
        Timber.e("Creating answer....")
        rtcClient.createAnswer(object : SimpleSdpObserver() {
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
    }

    fun setLocalAnswerDescription(remoteUid: String, localDescription: SessionDescription) {
        rtcClient.setLocalDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                Timber.e("Local description set!!, Creating Answer!!")
                sendAnswer(remoteUid, localDescription)
            }

            override fun onSetFailure(p0: String?) {
                super.onSetFailure(p0)
            }
        }, localDescription)
    }

    fun sendAnswer(remoteUid: String, localDescription: SessionDescription) {
        disposables += RtcAnswersRepository.getInstance()
            .create(remoteUid, localDescription)
            .subscribe {
                Timber.e("Answer sent to $remoteUid!!")
            }

    }

    val peerConnectionHandler: PeerConnectionHandler = object : PeerConnectionHandler {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            sendIceCandidate(iceCandidate)
        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
            removeIceCandidates(iceCandidates)
        }

        override fun onIceConnectionChanged(iceConnectionState: PeerConnection.IceConnectionState) {
            Timber.e("ICE Connection state changed : %s" , iceConnectionState.name)
        }
    }

    private fun startCallActivity(remoteUid: String) {
        val bundle = Bundle()
        bundle.putString("remoteUID", remoteUid)
        bundle.putBoolean("isCaller", false)
        rtcService.launchActivity<CallActivity>(bundle)
    }

}