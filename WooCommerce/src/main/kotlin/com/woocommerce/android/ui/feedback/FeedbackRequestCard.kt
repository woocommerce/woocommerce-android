package com.woocommerce.android.ui.feedback

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCElevatedConstraintLayout
import kotlinx.android.synthetic.main.feedback_request_card.view.*

class FeedbackRequestCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WCElevatedConstraintLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.feedback_request_card, this)
    }

    /**
     * Sets the click listeners for the buttons on this card
     *
     * @param negativeButtonAction The action to perform when the user clicks "Could be better"
     * @param positiveButtonAction The action to perform when the user clicks "I like it"
     */
    fun initView(negativeButtonAction: (() -> Unit), positiveButtonAction: (() -> Unit)) {
        btn_feedbackReq_negative.setOnClickListener { negativeButtonAction() }
        btn_feedbackReq_positive.setOnClickListener { positiveButtonAction() }
    }
}
