package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.android.synthetic.main.my_store_stats_reverted_notice.view.*

/**
 * Dashboard card that displays a reverted notice message if the WooCommerce Admin plugin
 * is disabled/uninstalled from a site but the v4 stats is already displayed to the user
 */
class MyStoreStatsRevertedNoticeCard @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.my_store_stats_reverted_notice, this)
    }

    fun initView(listener: MyStoreStatsAvailabilityListener) {
        btn_learn_more.setOnClickListener {
            // TODO: add analytics event here to track how many people click on the learn more button
            ChromeCustomTabUtils.launchUrl(context, context.getString(R.string.stats_woocommerce_admin_plugin_link))
        }

        btn_dismiss.setOnClickListener {
            // TODO: add analytics event here to track how many people click on the dismiss button
            listener.onMyStoreStatsRevertedNoticeCardDismissed()
        }
    }
}
