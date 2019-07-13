package com.tistory.mybstory.webrtc_example_kt.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding3.widget.textChangeEvents
import com.tistory.mybstory.webrtc_example_kt.R
import com.tistory.mybstory.webrtc_example_kt.base.BaseActivity
import com.tistory.mybstory.webrtc_example_kt.databinding.ActivityMainBinding
import com.tistory.mybstory.webrtc_example_kt.service.RtcService
import com.tistory.mybstory.webrtc_example_kt.ui.viewmodel.MainViewModel
import com.tistory.mybstory.webrtc_example_kt.util.extensions.await
import com.tistory.mybstory.webrtc_example_kt.util.extensions.hideKeyboard
import com.tistory.mybstory.webrtc_example_kt.util.extensions.launchActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : BaseActivity() {

    private val REQUEST_REQUIRED_PERMISSIONS = 0
    private val MANDATORY_PERMISSIONS = arrayOf(
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA"
    )
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val disposables = CompositeDisposable()
    private val sendIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel = binding.viewModel as MainViewModel
        initUI()
        checkPermissions()
        observeUser()
        signIn()
    }

    private fun observeUser() =
        viewModel.observeUser().observe(this, Observer {
            binding.tvUid.text = it.uid
            sendIntent.putExtra(Intent.EXTRA_TEXT, it.uid)
        })

    private fun initUI() {
        binding.btnStart.setOnClickListener {
            val bundle = Bundle().apply {
                putString("remoteUID", binding.etTarget.text.toString())
                putBoolean("isCaller", true)
            }
            Timber.d("Remote uid on Main: %s", binding.etTarget.text.toString())
            launchActivity<CallActivity>(bundle)
        }

        binding.ivShare.setOnClickListener {
            sendIntent.apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.share_to)))
        }

        binding.containerMain.setOnClickListener {
            it.hideKeyboard()
        }

        disposables += et_target.textChangeEvents().skipInitialValue()
            .subscribe {
                if (it.count == 0) {
                    til_target.error = "Enter remote id!"
                    btn_start.isEnabled = false
                } else {
                    til_target.error = ""
                    btn_start.isEnabled = true
                }
            }

    }

    fun signIn() {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.signIn().await()
            bindService()
        }
    }

    private fun bindService() {
        RtcService.startServiceWithContext(applicationContext)
    }

    private fun checkPermissions() =
        ActivityCompat.requestPermissions(this, MANDATORY_PERMISSIONS, REQUEST_REQUIRED_PERMISSIONS)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (permission in permissions) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission $permission is not granted", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
        }
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

}
