package com.woocommerce.android.ui.aisupportchat.diagnostics

import com.woocommerce.android.ui.troubleshooting.FailureType

/**
 * State of a single [DiagnosticTest] as it progresses through the
 * pending → running → passed/failed lifecycle.
 */
sealed interface TestStatus {
    data object Pending : TestStatus
    data object Running : TestStatus
    data object Passed : TestStatus
    data class Failed(
        val failureType: FailureType? = null,
        val technicalDetails: String? = null,
        val durationMs: Long = 0L
    ) : TestStatus
}
