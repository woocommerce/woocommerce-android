package com.woocommerce.android.ui.aisupportchat

import com.google.gson.JsonObject
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.util.BuildConfigWrapper
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SupportChatContextProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun buildInitialContext(diagnosticResult: DiagnosticResult? = null): JsonObject {
        val site = selectedSite.getIfExists()
        return JsonObject().apply {
            addProperty("platform", "android")
            addProperty("app_version", buildConfigWrapper.versionName)
            site?.let {
                if (it.siteId > 0L) {
                    addProperty("selectedSiteId", it.siteId)
                }
                addProperty("site_url", it.url)
            }
            diagnosticResult?.toTroubleshootingResults()?.let { troubleshootingResults ->
                addProperty("troubleshootingResults", troubleshootingResults)
            }
        }
    }

    private fun DiagnosticResult.toTroubleshootingResults(): String? =
        statuses
            .filter { it.status.isComplete }
            .takeIf { it.isNotEmpty() }
            ?.mapIndexed { index, status ->
                "## ${index + 1}. ${status.toTroubleshootingDescription()}"
            }
            ?.joinToString(separator = "\n\n")

    private fun DiagnosticStatus.toTroubleshootingDescription(): String {
        val result = when (val currentStatus = status) {
            TestStatus.Passed -> "Success"
            is TestStatus.Failed -> currentStatus.failureType?.name ?: "Failed"
            TestStatus.Pending,
            TestStatus.Running -> status.toWireValue()
        }
        val lines = mutableListOf(test.title, "Result: $result")
        (status as? TestStatus.Failed)?.technicalDetails?.let { details ->
            lines.add("Details: $details")
        }
        return lines.joinToString(separator = "\n")
    }

    private fun TestStatus.toWireValue(): String =
        when (this) {
            TestStatus.Pending -> "pending"
            TestStatus.Running -> "running"
            TestStatus.Passed -> "passed"
            is TestStatus.Failed -> "failed"
        }

    private val DiagnosticTest.title: String
        get() = when (this) {
            DiagnosticTest.INTERNET_CONNECTION -> "Internet Connection"
            DiagnosticTest.WPCOM_SERVERS -> "Connecting to WordPress.com Servers"
            DiagnosticTest.STORE_CONNECTION -> "Connecting to your site"
            DiagnosticTest.STORE_ORDERS -> "Fetching your site orders"
            DiagnosticTest.STORE_PRODUCTS -> "Fetching products in your store"
        }
}
