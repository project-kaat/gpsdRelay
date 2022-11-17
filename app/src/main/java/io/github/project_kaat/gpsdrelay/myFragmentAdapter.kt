package io.github.project_kaat.gpsdrelay

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class myFragmentAdapter (fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val TOTAL_TABS_N = 2

    override fun getItemCount(): Int {
        return TOTAL_TABS_N
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> mainTabFragment()
            1 -> settingsTabFragment()
            else -> mainTabFragment()
        }
    }
}