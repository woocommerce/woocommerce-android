package com.woocommerce.android.ui.login

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.woocommerce.android.R

class LoginPrologueAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    companion object {
        private const val NUM_PAGES = 4
    }

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

    override fun createFragment(position: Int): Fragment {
        return LoginPrologueViewPagerItemFragment.newInstance(
            drawableIds[position],
            stringIds[position]
        )
    }

    override fun getItemCount() = NUM_PAGES
}
