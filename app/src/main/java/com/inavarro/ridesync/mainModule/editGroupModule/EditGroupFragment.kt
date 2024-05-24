package com.inavarro.ridesync.mainModule.editGroupModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentEditGroupBinding

class EditGroupFragment : Fragment() {

    private lateinit var mBinding: FragmentEditGroupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentEditGroupBinding.inflate(layoutInflater)

        setupToolBar()

        return mBinding.root
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = "Editar grupo"
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
}