package com.inavarro.ridesync.mainModule.groupsModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentGroupsBinding
import com.inavarro.ridesync.mainModule.MainActivity

class GroupsFragment : Fragment() {

    private lateinit var mBinding: FragmentGroupsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentGroupsBinding.inflate(layoutInflater)

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(mBinding.tabLayout, mBinding.viewPager) { tab, position ->
            when(position){
                0 -> tab.text = "Subscritos"
                1 -> tab.text = "Explorar"
            }
        }.attach()
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).hideFragmentContainerViewActivity()
    }
}