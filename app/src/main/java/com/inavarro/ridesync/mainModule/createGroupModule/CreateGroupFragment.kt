package com.inavarro.ridesync.mainModule.createGroupModule

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
import com.inavarro.ridesync.databinding.FragmentCreateGroupBinding
import com.inavarro.ridesync.mainModule.MainActivity

class CreateGroupFragment : Fragment() {

    private lateinit var mBinding: FragmentCreateGroupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentCreateGroupBinding.inflate(layoutInflater)

        (activity as? MainActivity)?.hideBottomNav()

        setupToolBar()

        return mBinding.root
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = "Crear grupo"
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
}