package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.my_store_stats_availability_notice.view.*

class MyStoreStatsAvailabilityCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttrs: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttrs) {
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
            text = context.getString(R.string.my_store_stats_availability_title)
            textOff = context.getString(R.string.my_store_stats_availability_title)
            textOn = context.getString(R.string.my_store_stats_availability_title)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_gridicons_sync, 0, R.drawable.card_expander_selector, 0
            )
        }

        my_store_availability_message.setText(R.string.my_store_stats_availability_message)

        with(btn_primary) {
            text = context.getString(R.string.try_it_now)
            setOnClickListener {
                AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_AVAILABILITY_BANNER_TRY_TAPPED)
                listener.onMyStoreStatsAvailabilityAccepted()
            }
        }

        with(btn_secondary) {
            text = context.getString(R.string.no_thanks)
            setOnClickListener {
                AnalyticsTracker.track(Stat.DASHBOARD_NEW_STATS_AVAILABILITY_BANNER_CANCEL_TAPPED)
                listener.onMyStoreStatsAvailabilityRejected()
            }
        }
    }
}
