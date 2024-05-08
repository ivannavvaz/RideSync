package com.inavarro.ridesync.mainModule.shopModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inavarro.ridesync.databinding.FragmentShopBinding
import com.inavarro.ridesync.mainModule.MainActivity

class ShopFragment : Fragment() {

    private lateinit var mBinding: FragmentShopBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentShopBinding.inflate(layoutInflater)

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).hideFragmentContainerViewActivity()
    }
}