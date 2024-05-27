package com.inavarro.ridesync.mainModule.aboutModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentAboutBinding
import com.inavarro.ridesync.mainModule.MainActivity

class AboutFragment : Fragment() {

    private lateinit var mBinding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentAboutBinding.inflate(inflater, container, false)

        setupAboutFragment()

        setupToolBar()

        return mBinding.root
    }

    private fun setupAboutFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolbar)

        mBinding.toolbar.title = "Acerca de RideSync"
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
}