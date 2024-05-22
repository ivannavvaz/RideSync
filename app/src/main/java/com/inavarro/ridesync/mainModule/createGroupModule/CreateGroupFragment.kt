package com.inavarro.ridesync.mainModule.createGroupModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentCreateGroupBinding

class CreateGroupFragment : Fragment() {

    private lateinit var mBinding: FragmentCreateGroupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentCreateGroupBinding.inflate(layoutInflater)

        return mBinding.root
    }
}