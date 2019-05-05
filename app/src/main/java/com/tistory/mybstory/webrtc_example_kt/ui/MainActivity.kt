package com.tistory.mybstory.webrtc_example_kt.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tistory.mybstory.webrtc_example_kt.R
import com.tistory.mybstory.webrtc_example_kt.base.BaseActivity
import com.tistory.mybstory.webrtc_example_kt.databinding.ActivityMainBinding
import com.tistory.mybstory.webrtc_example_kt.ui.viewmodel.MainViewModel
import timber.log.Timber

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel =  ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel = binding.viewModel as MainViewModel
        observeUser()
        initUI()
    }

    private fun observeUser() = viewModel.observeUser().observe(this, Observer {
        binding.tvUid.text = it.uid
    })

    private fun initUI() {
        binding.btnStart.setOnClickListener{
            val bundle = Bundle()
            bundle.putString("remoteUID", binding.etTarget.text.toString())
            Timber.d("Remote uid on Main: %s", binding.etTarget.text.toString())
            launchActivity<CallActivity>(bundle)
        }
    }
}
