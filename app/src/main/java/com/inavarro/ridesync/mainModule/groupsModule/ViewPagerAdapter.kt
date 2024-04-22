package com.inavarro.ridesync.mainModule.groupsModule

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.inavarro.ridesync.mainModule.groupsModule.myGroups.MyGroupsFragment
import com.inavarro.ridesync.mainModule.groupsModule.searchGroups.SearchGroupsFragment

public class ViewPagerAdapter(groupsFragment: GroupsFragment) : FragmentStateAdapter(groupsFragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> MyGroupsFragment()
            1 -> SearchGroupsFragment()
            else -> MyGroupsFragment()
        }
    }
}