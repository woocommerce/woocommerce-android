package com.woocommerce.android.ui.aisupportchat.diagnostics

/**
 * Snapshot of an in-flight or completed diagnostics run for a given [issueType].
 *
 * `statuses` is the ordered list of all tests that will run for the issue type;
 * statuses transition pending → running → passed/failed in place as the run
 * progresses. After a failure the run halts and `suggestedAction` is populated.
 */
data class DiagnosticResult(
    val issueType: SupportIssueType,
    val statuses: List<Pair<DiagnosticTest, TestStatus>>,
    val suggestedAction: SuggestedFixAction? = null
) {
    val isComplete: Boolean
        get() = statuses.none { (_, status) -> status is TestStatus.Pending || status is TestStatus.Running }

    val firstFailure: Pair<DiagnosticTest, TestStatus.Failed>?
        get() = statuses.firstNotNullOfOrNull { (test, status) ->
            (status as? TestStatus.Failed)?.let { test to it }
        }
}
