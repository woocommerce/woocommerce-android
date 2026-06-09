package com.woocommerce.android.ui.aisupportchat.diagnostics

/**
 * Snapshot of an in-flight or completed diagnostics run for a given [issueType].
 *
 * `statuses` is the ordered list of all tests that will run for the issue type;
 * statuses transition pending → running → passed/failed in place as the run
 * progresses. After a failure the run halts and `suggestedAction` is populated
 * only when there is a concrete fix action available.
 */
data class DiagnosticResult(
    val issueType: SupportIssueType,
    val statuses: List<DiagnosticStatus>,
    val suggestedAction: SuggestedFixAction? = null
) {
    val isComplete: Boolean
        get() = statuses.all { it.status.isComplete }

    val firstFailure: DiagnosticStatus?
        get() = statuses.firstNotNullOfOrNull { diagnosticStatus ->
            (diagnosticStatus.status as? TestStatus.Failed)?.let { diagnosticStatus }
        }
}
