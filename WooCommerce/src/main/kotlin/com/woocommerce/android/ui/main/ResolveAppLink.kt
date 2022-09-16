package com.woocommerce.android.ui.main

import android.net.Uri
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PATH
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_URL
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class ResolveAppLink @Inject constructor(
    private val selectedSite: SelectedSite,
    private val crashLogging: CrashLogging,
    private val tracker: AnalyticsTrackerWrapper
) {

    operator fun invoke(uri: Uri?): Action {
        when {
            uri?.path?.contains("orders/details") == true -> {
                return prepareOrderDetailsEvent(uri)
            }
        }

        return Action.DoNothing
    }

    private fun prepareOrderDetailsEvent(data: Uri): Action {
        val (blogId, orderId) = try {
            (data.getParamOrNull("blog_id") to data.getParamOrNull("order_id"))
        } catch (e: NumberFormatException) {
            tracker.track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, properties = mapOf(KEY_URL to data.toString()))
            crashLogging.recordException(e, REPORT_CATEGORY)
            return Action.ViewStats
        }

        return when {
            blogId == null || orderId == null -> {
                tracker.track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, properties = mapOf(KEY_URL to data.toString()))
                crashLogging.recordEvent(message = "Malformed AppLink: $data", category = REPORT_CATEGORY)
                Action.ViewStats
            }
            !selectedSite.exists() -> {
                tracker.track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, properties = mapOf(KEY_URL to data.toString()))
                crashLogging.recordEvent(message = "User not logged in", category = REPORT_CATEGORY)
                Action.ViewStats
            }
            selectedSite.getIfExists()?.siteId != blogId -> {
                Action.ChangeSiteAndRestart(siteId = blogId, uri = data)
            }
            else -> {
                tracker.track(AnalyticsEvent.UNIVERSAL_LINK_OPENED, properties = mapOf(KEY_PATH to data.path.orEmpty()))
                Action.ViewOrderDetail(orderId = orderId)
            }
        }
    }

    private fun Uri.getParamOrNull(key: String) = getQueryParameter(key)?.toLong()

    sealed class Action {
        data class ViewOrderDetail(val orderId: Long) : Action()
        data class ChangeSiteAndRestart(val siteId: Long, val uri: Uri) : Action()
        object ViewStats : Action()
        object DoNothing : Action()
    }

    companion object {
        private const val REPORT_CATEGORY = "AppLinks"
    }
}
