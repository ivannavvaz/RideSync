package com.inavarro.ridesync.mainModule.homeModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentHomeBinding
import com.inavarro.ridesync.mainModule.MainActivity

class HomeFragment : Fragment() {

    private lateinit var mBinding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentHomeBinding.inflate(layoutInflater)

        // Hide main tab layout
        (activity as? MainActivity)?.hideTabLayout()

        return mBinding.root
    }
}