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
import kotlinx.android.synthetic.main.my_store_stats_reverted_notice.view.*

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
        View.inflate(context, R.layout.my_store_stats_reverted_notice, this)
    }

    fun initView(listener: MyStoreStatsAvailabilityListener) {
        my_store_reverted_viewMore.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                WooAnimUtils.fadeIn(my_store_reverted_morePanel)
            } else {
                WooAnimUtils.fadeOut(my_store_reverted_morePanel)
            }
        }

        btn_learn_more.setOnClickListener {
            AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_REVERTED_BANNER_LEARN_MORE_TAPPED)
            ChromeCustomTabUtils.launchUrl(context, AppUrls.WOOCOMMERCE_PLUGIN)
        }

        btn_dismiss.setOnClickListener {
            AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_REVERTED_BANNER_DISMISS_TAPPED)
            listener.onMyStoreStatsRevertedNoticeCardDismissed()
        }
    }
}
