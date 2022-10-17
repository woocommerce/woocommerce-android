package com.woocommerce.android.ui.login

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.woocommerce.android.R

class LoginPrologueAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val drawableIds = arrayOf(
        R.drawable.img_prologue_products,
        R.drawable.img_prologue_orders,
        R.drawable.img_prologue_analytics
    )

    private val titleIds = arrayOf(
        R.string.login_prologue_label_products,
        R.string.login_prologue_label_orders,
        R.string.login_prologue_label_analytics
    )

    private val subtitleIds = arrayOf(
        R.string.login_prologue_label_products_subtitle,
        R.string.login_prologue_label_orders_subtitle,
        R.string.login_prologue_label_analytics_subtitle
    )

    override fun createFragment(position: Int): Fragment {
        return LoginPrologueViewPagerItemFragment.newInstance(
            drawableIds[position],
            titleIds[position],
            subtitleIds[position]
        )
    }

    override fun getItemCount() = drawableIds.size
}
