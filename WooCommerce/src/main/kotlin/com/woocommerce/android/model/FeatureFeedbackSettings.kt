package com.woocommerce.android.model

import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.extensions.greaterThan
import com.woocommerce.android.extensions.pastTimeDeltaFromNowInDays
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import java.util.Calendar
import java.util.Date

data class FeatureFeedbackSettings(
    val feature: Feature,
    val feedbackState: FeedbackState = UNANSWERED,
    val settingChangeDate: Long = Calendar.getInstance().time.time,
) {
    val key
        get() = feature.toString()

    fun registerItself(feedbackPrefs: FeedbackPrefs) = feedbackPrefs.setFeatureFeedbackSettings(this)

    enum class FeedbackState {
        GIVEN,
        DISMISSED,
        UNANSWERED
    }

    enum class Feature {
        SHIPPING_LABEL_M4,
        PRODUCT_ADDONS,
        SIMPLE_PAYMENTS_AND_ORDER_CREATION,
        COUPONS,
        ANALYTICS_HUB,
        TAP_TO_PAY,
        ORDER_SHIPPING_LINES
    }

    fun isFeedbackMoreThanDaysAgo(days: Int) = Date(settingChangeDate).pastTimeDeltaFromNowInDays greaterThan days

    fun isFeedbackGivenMoreThanDaysAgo(days: Int) =
        feedbackState == FeedbackState.GIVEN &&
            Date(settingChangeDate).pastTimeDeltaFromNowInDays greaterThan days
}
