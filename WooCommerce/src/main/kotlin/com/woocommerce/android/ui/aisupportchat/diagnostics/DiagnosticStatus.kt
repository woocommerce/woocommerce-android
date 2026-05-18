package com.woocommerce.android.ui.aisupportchat.diagnostics

/**
 * Status row for one diagnostic test in a diagnostics run.
 */
data class DiagnosticStatus(
    val test: DiagnosticTest,
    val status: TestStatus
)
