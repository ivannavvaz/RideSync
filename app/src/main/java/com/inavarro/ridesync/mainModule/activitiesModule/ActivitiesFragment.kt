package com.inavarro.ridesync.mainModule.activitiesModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentActivitiesBinding

class ActivitiesFragment : Fragment() {

    private lateinit var mBinding: FragmentActivitiesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentActivitiesBinding.inflate(layoutInflater)

        return mBinding.root
    }
}