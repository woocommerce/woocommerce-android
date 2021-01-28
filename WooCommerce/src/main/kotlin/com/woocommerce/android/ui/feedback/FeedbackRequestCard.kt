package com.woocommerce.android.ui.feedback

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_LIKED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_NOT_LIKED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_SHOWN
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.APP_FEEDBACK_PROMPT
import com.woocommerce.android.databinding.FeedbackRequestCardBinding
import com.woocommerce.android.widgets.WCElevatedConstraintLayout

class FeedbackRequestCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WCElevatedConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = FeedbackRequestCardBinding.inflate(LayoutInflater.from(ctx))

    /**
     * Sets the click listeners for the buttons on this card
     *
     * @param negativeButtonAction The action to perform when the user clicks "Could be better"
     * @param positiveButtonAction The action to perform when the user clicks "I like it"
     */
    fun initView(negativeButtonAction: (() -> Unit), positiveButtonAction: (() -> Unit)) {
        AnalyticsTracker.track(
            APP_FEEDBACK_PROMPT,
            mapOf(KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_SHOWN)
        )

        binding.btnFeedbackReqNegative.setOnClickListener {
            AnalyticsTracker.track(
                APP_FEEDBACK_PROMPT,
                mapOf(KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_NOT_LIKED)
            )
            negativeButtonAction()
        }

        binding.btnFeedbackReqPositive.setOnClickListener {
            AnalyticsTracker.track(
                APP_FEEDBACK_PROMPT,
                mapOf(KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_LIKED)
            )
            positiveButtonAction()
        }
    }
}
