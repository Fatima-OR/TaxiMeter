package com.cmc.taximeter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity, private val userEmail: String) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MeterFragment()
            1 -> MapsFragment()
            2 -> UserFragment().apply {
                arguments = Bundle().apply {
                    putString("userEmail", userEmail)
                }
            }
            else -> MeterFragment()
        }
    }
}