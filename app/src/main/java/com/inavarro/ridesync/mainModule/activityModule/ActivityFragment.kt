package com.inavarro.ridesync.mainModule.activityModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentActivityBinding
import com.inavarro.ridesync.mainModule.MainActivity

class ActivityFragment : Fragment() {

    private lateinit var mBinding: FragmentActivityBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentActivityBinding.inflate(inflater, container, false)

        setupShopFragment()

        setupToolBar()

        return mBinding.root
    }

    private fun setupShopFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolbar)

        mBinding.toolbar.title = ""
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
}