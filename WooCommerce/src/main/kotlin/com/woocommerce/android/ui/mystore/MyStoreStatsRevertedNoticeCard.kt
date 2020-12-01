package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.MyStoreStatsRevertedNoticeBinding
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooAnimUtils

/**
 * Dashboard card that displays a reverted notice message if the WooCommerce Admin plugin
 * is disabled/uninstalled from a site but the v4 stats is already displayed to the user
 */
class MyStoreStatsRevertedNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = MyStoreStatsRevertedNoticeBinding.inflate(LayoutInflater.from(ctx), this)

    fun initView(listener: MyStoreStatsAvailabilityListener) {
        binding.myStoreRevertedViewMore.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                WooAnimUtils.fadeIn(binding.myStoreRevertedMorePanel)
            } else {
                WooAnimUtils.fadeOut(binding.myStoreRevertedMorePanel)
            }
        }

        binding.btnLearnMore.setOnClickListener {
            AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_REVERTED_BANNER_LEARN_MORE_TAPPED)
            ChromeCustomTabUtils.launchUrl(context, AppUrls.WOOCOMMERCE_PLUGIN)
        }

        binding.btnDismiss.setOnClickListener {
            AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_REVERTED_BANNER_DISMISS_TAPPED)
            listener.onMyStoreStatsRevertedNoticeCardDismissed()
        }
    }
}
