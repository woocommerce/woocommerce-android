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
        return when {
            uri?.path?.contains("orders/details") == true -> {
                prepareOrderDetailsAction(uri)
            }
            uri?.path?.contains("payments") == true -> {
                preparePaymentsAction(uri)
            }
            else -> Action.DoNothing
        }
    }

    private fun preparePaymentsAction(data: Uri): Action {
        val blogId = try {
            data.getParamOrNull("blog_id")
        } catch (e: NumberFormatException) {
            trackLinkFailure(data)
            crashLogging.recordException(e, REPORT_CATEGORY)
            return Action.ViewStats
        }

        return when {
            blogId == null -> {
                trackLinkSuccess(data)
                Action.ViewPayments
            }
            !selectedSite.exists() -> {
                trackLinkFailure(data)
                Action.ViewStats
            }
            selectedSite.getIfExists()?.siteId != blogId -> {
                Action.ChangeSiteAndRestart(siteId = blogId, uri = data)
            }
            else -> {
                trackLinkSuccess(data)
                Action.ViewPayments
            }
        }
    }

    private fun prepareOrderDetailsAction(data: Uri): Action {
        val (blogId, orderId) = try {
            (data.getParamOrNull("blog_id") to data.getParamOrNull("order_id"))
        } catch (e: NumberFormatException) {
            trackLinkFailure(data)
            crashLogging.recordException(e, REPORT_CATEGORY)
            return Action.ViewStats
        }

        return when {
            blogId == null || orderId == null -> {
                trackLinkFailure(data)
                crashLogging.recordEvent(message = "Malformed AppLink: $data", category = REPORT_CATEGORY)
                Action.ViewStats
            }
            !selectedSite.exists() -> {
                trackLinkFailure(data)
                Action.ViewStats
            }
            selectedSite.getIfExists()?.siteId != blogId -> {
                Action.ChangeSiteAndRestart(siteId = blogId, uri = data)
            }
            else -> {
                trackLinkSuccess(data)
                Action.ViewOrderDetail(orderId = orderId)
            }
        }
    }

    private fun trackLinkSuccess(data: Uri) =
        tracker.track(AnalyticsEvent.UNIVERSAL_LINK_OPENED, properties = mapOf(KEY_PATH to data.path.orEmpty()))

    private fun trackLinkFailure(data: Uri) =
        tracker.track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, properties = mapOf(KEY_URL to data.toString()))

    private fun Uri.getParamOrNull(key: String) = getQueryParameter(key)?.toLong()

    sealed class Action {
        data class ViewOrderDetail(val orderId: Long) : Action()
        data class ChangeSiteAndRestart(val siteId: Long, val uri: Uri) : Action()
        object ViewStats : Action()
        object ViewPayments : Action()
        object DoNothing : Action()
    }

    companion object {
        private const val REPORT_CATEGORY = "AppLinks"
    }
}
