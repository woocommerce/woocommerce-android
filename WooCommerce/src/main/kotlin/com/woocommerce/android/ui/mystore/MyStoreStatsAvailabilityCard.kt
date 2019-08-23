package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.my_store_stats_availability_notice.view.*

class MyStoreStatsAvailabilityCard @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.my_store_stats_availability_notice, this)
    }

    fun initView(listener: MyStoreStatsAvailabilityListener) {
        my_store_availability_viewMore.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                my_store_availability_morePanel.visibility = View.VISIBLE
            } else {
                my_store_availability_morePanel.visibility = View.GONE
            }
        }

        btn_try.setOnClickListener {
            // TODO: add analytics event here to track how many people click on the try now button
            listener.onMyStoreStatsAvailabilityAccepted()
        }

        btn_no_thanks.setOnClickListener {
            // TODO: add analytics event here to track how many people click on the try now button
            listener.onMyStoreStatsAvailabilityRejected()
        }
    }
}
