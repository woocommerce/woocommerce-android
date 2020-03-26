package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.my_store_stats_availability_notice.view.*

/**
 * Dashboard card that displays a reverted notice message if the WooCommerce Admin plugin
 * is disabled/uninstalled from a site but the v4 stats is already displayed to the user
 */
class MyStoreStatsRevertedNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.my_store_stats_availability_notice, this)
    }

    fun initView(listener: MyStoreStatsAvailabilityListener) {
        my_store_availability_viewMore.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                WooAnimUtils.fadeIn(my_store_availability_morePanel)
            } else {
                WooAnimUtils.fadeOut(my_store_availability_morePanel)
            }
        }
        with(my_store_availability_viewMore) {
            text = context.getString(R.string.my_store_stats_reverted_title)
            textOff = context.getString(R.string.my_store_stats_reverted_title)
            textOn = context.getString(R.string.my_store_stats_reverted_title)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_gridicons_sync, 0, R.drawable.card_expander_selector, 0
            )
        }

        my_store_availability_message.setText(R.string.my_store_stats_reverted_message)

        with(btn_primary) {
            text = context.getString(R.string.learn_more)
            setOnClickListener {
                AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_REVERTED_BANNER_LEARN_MORE_TAPPED)
                ChromeCustomTabUtils.launchUrl(context, AppUrls.WOOCOMMERCE_PLUGIN)
            }
        }

        with(btn_secondary) {
            text = context.getString(R.string.dismiss)
            setOnClickListener {
                AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_REVERTED_BANNER_DISMISS_TAPPED)
                listener.onMyStoreStatsRevertedNoticeCardDismissed()
            }
        }
    }
}
