package com.realsoc.mechanicalcounterviewsample

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.realsoc.mechanicalcounterview.MechanicalCounterView
import com.realsoc.mechanicalcounterviewsample.databinding.ActivityMechanicalCounterSampleBinding

class MechanicalCounterSampleActivity : AppCompatActivity(), View.OnClickListener, MechanicalCounterView.OnCountStopListener {

    override fun onCountStop(count: Int) {
        Log.e("Count is ", count.toString())
        //binding.counter.start()
    }

    lateinit var binding: ActivityMechanicalCounterSampleBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mechanical_counter_sample)
        binding.counter.onCountTerminatedListener = this
        binding.clickListener = this
        binding.counter.autoStart = true
        binding.counter.currentValue = 200
        binding.counter.duration = 3000
        binding.counter.goal = 1000
        //binding.counter.start()
    }

    override fun onClick(v: View?) {
        binding.counter.goal = 2000
    }
}
