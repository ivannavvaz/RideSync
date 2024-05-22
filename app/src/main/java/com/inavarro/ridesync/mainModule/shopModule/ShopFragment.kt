package com.inavarro.ridesync.mainModule.shopModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.inavarro.ridesync.R
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

        setupShopFragment()

        setupToolBar()

        return mBinding.root
    }

    private fun setupShopFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = ""
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
}