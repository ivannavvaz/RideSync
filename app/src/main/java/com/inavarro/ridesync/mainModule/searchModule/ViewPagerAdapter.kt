package com.inavarro.ridesync.mainModule.searchModule

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.inavarro.ridesync.mainModule.searchModule.searchActivities.SearchActivitiesFragment
import com.inavarro.ridesync.mainModule.searchModule.searchGroups.SearchGroupsFragment

public class ViewPagerAdapter(searchFragment: SearchFragment) : FragmentStateAdapter(searchFragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> SearchActivitiesFragment()
            1 -> SearchGroupsFragment()
            else -> SearchActivitiesFragment()
        }
    }
}