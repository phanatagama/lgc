package com.deepid.lgc.ui.main

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.deepid.lgc.ui.main.fragment.GraphicfieldFragment
import com.deepid.lgc.ui.main.fragment.OverallFragment
import com.deepid.lgc.ui.main.fragment.TextfieldFragment

class SectionsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    var documentReaderResults: Parcelable? = null
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        var fragment: Fragment? = null
        when (position) {
            0 -> fragment = OverallFragment.newInstance(documentReaderResults)
            1 -> fragment = TextfieldFragment.newInstance(documentReaderResults)
            2 -> fragment = GraphicfieldFragment.newInstance()

        }
        return fragment as Fragment
    }


}