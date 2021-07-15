package com.woocommerce.android.ui.login

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCViewPager

class LoginPrologueViewPager : WCViewPager {
    companion object {
        private const val NUM_PAGES = 4
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun initViewPager(fm: FragmentManager) {
        adapter = ViewPagerAdapter(fm)
    }

    private inner class ViewPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
        private val drawableIds = arrayOf(
            R.drawable.img_prologue_analytics,
            R.drawable.img_prologue_orders,
            R.drawable.img_prologue_products,
            R.drawable.img_prologue_reviews
        )

        private val stringIds = arrayOf(
            R.string.login_prologue_label_analytics,
            R.string.login_prologue_label_orders,
            R.string.login_prologue_label_products,
            R.string.login_prologue_label_reviews
        )

        override fun getItem(position: Int): Fragment {
            return LoginPrologueViewPagerItemFragment.newInstance(
                drawableIds[position],
                stringIds[position]
            )
        }

        override fun getCount() = NUM_PAGES
    }
}
