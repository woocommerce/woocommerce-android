package com.woocommerce.android.ui.aisupportchat.diagnostics

import com.woocommerce.android.ui.troubleshooting.FailureType

/**
 * State of a single [DiagnosticTest] as it progresses through the
 * pending → running → passed/failed lifecycle.
 */
sealed interface TestStatus {
    val isComplete: Boolean

    data object Pending : TestStatus {
        override val isComplete = false
    }

    data object Running : TestStatus {
        override val isComplete = false
    }

    data object Passed : TestStatus {
        override val isComplete = true
    }

    data class Failed(
        val failureType: FailureType? = null,
        val technicalDetails: String? = null,
        val durationMs: Long = 0L
    ) : TestStatus {
        override val isComplete = true
    }
}
