package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.util.BuildConfigWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class SupportChatContextProviderTest {
    private val selectedSite: SelectedSite = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()

    private val contextProvider = SupportChatContextProvider(
        selectedSite = selectedSite,
        buildConfigWrapper = buildConfigWrapper
    )

    @Test
    fun `given no selected site, when building context, then site fields are omitted`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val context = contextProvider.buildInitialContext()

        assertThat(context["platform"].asString).isEqualTo("android")
        assertThat(context["app_version"].asString).isEqualTo(APP_VERSION)
        assertThat(context.has("selectedSiteId")).isFalse()
        assertThat(context.has("site_url")).isFalse()
    }

    @Test
    fun `given selected site, when building context, then selected site fields use support chat names`() {
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = SITE_ID
                id = LOCAL_SITE_ID
                url = SITE_URL
            }
        )
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val result = contextProvider.buildInitialContext()

        assertThat(result.get("selectedSiteId").asLong).isEqualTo(SITE_ID)
        assertThat(result.get("site_url").asString).isEqualTo(SITE_URL)
        assertThat(result.has("site_id")).isFalse
        assertThat(result.has("local_site_id")).isFalse
    }

    @Test
    fun `given selected site without remote id, when building context, then selected site id is omitted`() {
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = 0L
                id = LOCAL_SITE_ID
                url = SITE_URL
            }
        )
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val result = contextProvider.buildInitialContext()

        assertThat(result.has("selectedSiteId")).isFalse
        assertThat(result.get("site_url").asString).isEqualTo(SITE_URL)
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

        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = SITE_ID
                url = SITE_URL
            }
        )
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val result = contextProvider.buildInitialContext(diagnosticResult = diagnostics)

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
        const val APP_VERSION = "1.2.3"
        const val SITE_ID = 20L
        const val LOCAL_SITE_ID = 456
        const val SITE_URL = "https://example.com"
    }
}
