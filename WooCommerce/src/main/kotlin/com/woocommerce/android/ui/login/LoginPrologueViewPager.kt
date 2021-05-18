package com.woocommerce.android.ui.login

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCViewPager

class LoginPrologueViewPager : WCViewPager {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private fun initViewPager(fm: FragmentManager) {
        val adapter = ViewPagerAdapter(fm)
    }

    private inner class ViewPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
        private val drawableIds = arrayOf(
            R.drawable.img_prologue_analytics,
            R.drawable.img_prologue_orders,
            R.drawable.img_prologue_products,
            R.drawable.img_prologue_reviews
        )

        override fun getItem(position: Int): Fragment {
            return LoginPrologueViewPagerFragment.newInstance(drawableIds[position])
        }

        override fun getCount(): Int {
            return drawableIds.size
        }
    }
}
