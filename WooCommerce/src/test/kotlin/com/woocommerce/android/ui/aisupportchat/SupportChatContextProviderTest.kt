package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

class SupportChatContextProviderTest {
    private val site: SiteModel = mock {
        on { siteId } doReturn SITE_ID
        on { url } doReturn SITE_URL
    }
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val provider = SupportChatContextProvider(selectedSite)

    @Test
    fun `when context is built, then selected site fields use support chat names`() {
        val result = provider.buildInitialContext()

        assertThat(result.get("selectedSiteId").asLong).isEqualTo(SITE_ID)
        assertThat(result.get("site_url").asString).isEqualTo(SITE_URL)
        assertThat(result.has("site_id")).isFalse
        assertThat(result.has("local_site_id")).isFalse
    }

    @Test
    fun `given diagnostics, when context is built, then troubleshooting results are formatted as string`() {
        val diagnostics = DiagnosticResult(
            issueType = SupportIssueType.LOADING_ORDERS,
            statuses = listOf(
                DiagnosticStatus(DiagnosticTest.INTERNET_CONNECTION, TestStatus.Passed),
                DiagnosticStatus(
                    test = DiagnosticTest.WPCOM_SERVERS,
                    status = TestStatus.Failed(technicalDetails = "WPCom 503")
                ),
                DiagnosticStatus(DiagnosticTest.STORE_CONNECTION, TestStatus.Pending)
            )
        )

        val result = provider.buildInitialContext(diagnosticResult = diagnostics)

        assertThat(result.get("troubleshootingResults").asString).isEqualTo(
            """
            ## 1. Internet Connection
            Result: Success

            ## 2. Connecting to WordPress.com Servers
            Result: Failed
            Details: WPCom 503
            """.trimIndent()
        )
        assertThat(result.has("diagnostics")).isFalse
        assertThat(result.has("support_issue_type")).isFalse
    }

    private companion object {
        const val SITE_ID = 20L
        const val SITE_URL = "https://example.com"
    }
}
