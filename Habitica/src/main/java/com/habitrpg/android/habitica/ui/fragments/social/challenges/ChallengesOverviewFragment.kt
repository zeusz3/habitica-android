package com.habitrpg.android.habitica.ui.fragments.social.challenges

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.components.AppComponent
import com.habitrpg.android.habitica.data.ChallengeRepository
import com.habitrpg.android.habitica.ui.fragments.BaseMainFragment
import com.habitrpg.android.habitica.ui.helpers.bindView
import com.habitrpg.android.habitica.ui.helpers.resetViews

import javax.inject.Inject

class ChallengesOverviewFragment : BaseMainFragment() {

    @Inject
    internal lateinit var challengeRepository: ChallengeRepository

    private val viewPager: ViewPager? by bindView(R.id.viewPager)
    var statePagerAdapter: FragmentStatePagerAdapter? = null
    private var userChallengesFragment: ChallengeListFragment? = ChallengeListFragment()
    private var availableChallengesFragment: ChallengeListFragment? = ChallengeListFragment()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        this.usesTabLayout = true
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_viewpager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resetViews()

        userChallengesFragment?.user = this.user
        userChallengesFragment?.setViewUserChallengesOnly(true)

        availableChallengesFragment?.user = this.user
        availableChallengesFragment?.setViewUserChallengesOnly(false)
        setViewPagerAdapter()
    }

    override fun onDestroy() {
        challengeRepository.close()
        super.onDestroy()
    }

    override fun injectFragment(component: AppComponent) {
        component.inject(this)
    }

    private fun setViewPagerAdapter() {
        val fragmentManager = childFragmentManager

        statePagerAdapter = object : FragmentStatePagerAdapter(fragmentManager) {

            override fun getItem(position: Int): Fragment? {
                val fragment = Fragment()

                return when (position) {
                    0 -> userChallengesFragment
                    1 -> availableChallengesFragment
                    else -> fragment
                }
            }

            override fun getCount(): Int {
                return 2
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when (position) {
                    0 -> getString(R.string.my_challenges)
                    1 -> getString(R.string.public_challenges)
                    else -> ""
                }
            }
        }
        viewPager?.adapter = statePagerAdapter
        tabLayout?.setupWithViewPager(viewPager)
        statePagerAdapter?.notifyDataSetChanged()
    }

    override fun customTitle(): String {
        return if (!isAdded) {
            ""
        } else getString(R.string.challenges)
    }
}
