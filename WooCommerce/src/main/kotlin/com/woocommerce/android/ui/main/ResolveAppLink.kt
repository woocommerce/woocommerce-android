package com.woocommerce.android.ui.main

import android.net.Uri
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class ResolveAppLink @Inject constructor(
    private val selectedSite: SelectedSite,
    private val crashLogging: CrashLogging
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
            (data.getQueryParameter("blog_id")?.toLong() to data.getQueryParameter("order_id")?.toLong())
        } catch (e: NumberFormatException) {
            crashLogging.recordException(e, REPORT_CATEGORY)
            return Action.ViewStats
        }

        return when {
            blogId == null || orderId == null -> {
                crashLogging.recordEvent(message = "Malformed AppLink: $data", category = REPORT_CATEGORY)
                Action.ViewStats
            }
            !selectedSite.exists() -> {
                crashLogging.recordEvent(message = "User not logged in", category = REPORT_CATEGORY)
                Action.ViewStats
            }
            selectedSite.getIfExists()?.siteId != blogId -> {
                Action.ChangeSiteAndRestart(siteId = blogId, uri = data)
            }
            else -> {
                Action.ViewOrderDetail(orderId = orderId)
            }
        }
    }

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
