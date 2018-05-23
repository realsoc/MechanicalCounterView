package com.realsoc.mechanicalcounterviewsample

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.realsoc.mechanicalcounterview.MechanicalCounterView
import com.realsoc.mechanicalcounterviewsample.databinding.ActivityMechanicalCounterSampleBinding

class MechanicalCounterSampleActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityMechanicalCounterSampleBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mechanical_counter_sample)
        binding.clickListener = this
    }

    override fun onClick(v: View?) {
        binding.counter.start()
        binding.counter.onCountTerminatedListener = object:  MechanicalCounterView.OnCountTerminatedListener {
            override fun onCountTerminated() {
                Log.d("MechanicalCounterSample", "Animation terminated")
            }

        }
    }
}
