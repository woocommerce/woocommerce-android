package com.woocommerce.android.ui.feedback

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCElevatedConstraintLayout

class FeedbackRequestCard @JvmOverloads constructor(
    ctx: Context, attrs:
    AttributeSet? = null,
    defStyleAttr: Int = 0
) : WCElevatedConstraintLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.feedback_request_card, this)
    }
}
