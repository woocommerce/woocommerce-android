package com.woocommerce.android.ui.network

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import org.wordpress.android.util.NetworkUtils

class OfflineStatusBarView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : FrameLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.offline_status_bar, this)
        setOnClickListener {
            WooAnimUtils.animateBottomBar(this, false)
            postDelayed({
                if (NetworkUtils.isNetworkAvailable(context)) {
                    hide()
                } else {
                    show()
                }
            }, 2000)
        }
    }

    fun show() {
        if (visibility != View.VISIBLE) {
            WooAnimUtils.animateBottomBar(this, show = true)
        }
    }

    fun hide() {
        if (visibility == View.VISIBLE) {
            WooAnimUtils.animateBottomBar(this, show = false)
        }
    }
}
