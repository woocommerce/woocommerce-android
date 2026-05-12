package com.woocommerce.android.ui.aisupportchat

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.SuggestedFixAction
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import dagger.Reusable
import javax.inject.Inject

@Reusable
class SupportChatContextProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun buildInitialContext(
        issueType: SupportIssueType? = null,
        diagnosticResult: DiagnosticResult? = null
    ): JsonObject {
        val site = selectedSite.get()
        return JsonObject().apply {
            addProperty("platform", "android")
            addProperty("app_version", buildConfigWrapper.versionName)
            addProperty("site_id", site.siteId)
            addProperty("local_site_id", site.id)
            addProperty("site_url", site.url)
            issueType?.let { addProperty("support_issue_type", it.name.lowercase()) }
            diagnosticResult?.let { add("diagnostics", it.toJson()) }
        }
    }

    private fun DiagnosticResult.toJson(): JsonObject =
        JsonObject().apply {
            addProperty("issue_type", issueType.name.lowercase())
            addProperty("is_complete", isComplete)
            suggestedAction?.let { addProperty("suggested_action", it.toWireValue()) }
            firstFailure?.let { add("first_failure", it.toJson()) }
            add(
                "statuses",
                JsonArray().apply {
                    statuses.forEach { add(it.toJson()) }
                }
            )
        }

    private fun DiagnosticStatus.toJson(): JsonObject =
        JsonObject().apply {
            addProperty("test", test.name.lowercase())
            addProperty("status", status.toWireValue())
            if (status is TestStatus.Failed) {
                status.failureType?.let { addProperty("failure_type", it.name.lowercase()) }
                status.technicalDetails?.let { addProperty("technical_details", it) }
                addProperty("duration_ms", status.durationMs)
            }
        }

    private fun TestStatus.toWireValue(): String =
        when (this) {
            TestStatus.Pending -> "pending"
            TestStatus.Running -> "running"
            TestStatus.Passed -> "passed"
            is TestStatus.Failed -> "failed"
        }

    private fun SuggestedFixAction.toWireValue(): String =
        when (this) {
            SuggestedFixAction.RetryDiagnostics -> "retry_diagnostics"
        }
}
