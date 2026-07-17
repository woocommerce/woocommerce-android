package com.woocommerce.android.ui.troubleshooting

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectivityDiagnosticLogTest {
    @Test
    fun `given no completed checks, when generating the log, then it returns null`() {
        val checks = listOf(
            ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, ConnectivityCheckStatus.NotStarted),
            ConnectivityCheckCardData(ConnectivityCheckType.STORE, ConnectivityCheckStatus.InProgress)
        )

        assertThat(checks.toConnectivityDiagnosticLog()).isNull()
    }

    @Test
    fun `given completed checks, when generating the log, then only completed ones are formatted`() {
        val checks = listOf(
            ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, ConnectivityCheckStatus.Success(durationMs = 12)),
            ConnectivityCheckCardData(
                ConnectivityCheckType.WP_COM,
                ConnectivityCheckStatus.Failure(
                    error = FailureType.JETPACK,
                    technicalDetails = "boom",
                    durationMs = 34
                )
            ),
            ConnectivityCheckCardData(ConnectivityCheckType.STORE, ConnectivityCheckStatus.InProgress)
        )

        val log = checks.toConnectivityDiagnosticLog()

        assertThat(log).isNotNull()
        assertThat(log).contains("Took: 12ms")
        assertThat(log).contains("Result: Success")
        assertThat(log).contains("Took: 34ms")
        assertThat(log).contains("Result: JETPACK")
        assertThat(log).contains("boom")
        // The in-progress check is excluded, so only the two completed checks are numbered.
        assertThat(log!!.lines().count { it.startsWith("## ") }).isEqualTo(2)
    }
}
