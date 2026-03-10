package com.woocommerce.android.ui.bookings

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper

class BookingAnalyticsHelper {
    fun AnalyticsTrackerWrapper.trackError(
        event: AnalyticsEvent,
        throwable: Throwable,
        errorContext: String,
        additionalProperties: Map<String, Any> = emptyMap()
    ) {
        track(
            stat = event,
            properties = buildMap {
                throwable.errorCode()?.let { code -> put(AnalyticsTracker.KEY_ERROR_CODE, code) }
                putAll(additionalProperties)
            },
            errorContext = errorContext,
            errorType = throwable.errorType(),
            errorDescription = throwable.message
        )
    }

    private fun Throwable.errorCode(): String? = (this as? WooException)?.error?.apiErrorCode

    private fun Throwable.errorType(): String? = when (this) {
        is WooException -> error.type.name
        else -> javaClass.simpleName
    }

    companion object {
        const val KEY_BOOKING_STATUS = "booking_status"
        const val KEY_ACTION = "action"
        const val KEY_SELECTED_TAB = "selected_tab"
        const val KEY_IS_SEARCH_ACTIVE = "is_search_active"
        const val KEY_IS_FILTERING_ACTIVE = "is_filtering_active"
        const val KEY_SORT_OPTION = "sort_option"
        const val KEY_IS_DEFAULT_TAB = "is_default_tab"
        const val KEY_IS_LIST_EMPTY = "is_list_empty"
        const val KEY_IS_FILTERED = "is_filtered"
        const val KEY_SELECTED_FILTERS = "selected_filters"
    }
}
