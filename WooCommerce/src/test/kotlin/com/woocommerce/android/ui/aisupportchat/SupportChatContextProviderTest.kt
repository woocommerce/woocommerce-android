package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.logs.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.concurrent.TimeUnit

class SupportChatContextProviderTest {
    private val selectedSite: SelectedSite = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val mobileStatusProvider: MobileStatusProvider = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()
    private val wooLog: WooLog = mock()

    private val contextProvider = SupportChatContextProvider(
        selectedSite = selectedSite,
        buildConfigWrapper = buildConfigWrapper,
        mobileStatusProvider = mobileStatusProvider,
        dispatchers = CoroutineDispatchers(
            main = Dispatchers.Unconfined,
            computation = Dispatchers.Unconfined,
            io = Dispatchers.Unconfined
        ),
        unifiedLoginTracker = unifiedLoginTracker,
        wooLog = wooLog,
        clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
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
    fun `given launch site address without selected site, when building context, then site url is included`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val result = contextProvider.buildInitialContext(siteAddress = SITE_URL)

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
                DiagnosticStatus(DiagnosticTest.ANALYTICS_SETTING, TestStatus.Passed),
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

            ## 3. Checking analytics setting
            Result: Success
            """.trimIndent()
        )
        assertThat(result.has("diagnostics")).isFalse
        assertThat(result.has("support_issue_type")).isFalse
    }

    @Test
    fun `given diagnostics and status report, when context is built, then report follows the diagnostics`() {
        val diagnostics = DiagnosticResult(
            issueType = SupportIssueType.OTHER,
            statuses = listOf(DiagnosticStatus(DiagnosticTest.INTERNET_CONNECTION, TestStatus.Passed))
        )
        whenever(selectedSite.getIfExists()).thenReturn(null)
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val result = contextProvider.buildInitialContext(
            diagnosticResult = diagnostics,
            siteAddress = SITE_URL,
            statusReport = STATUS_REPORT
        )

        assertThat(result.get("troubleshootingResults").asString).isEqualTo(
            "## 1. Internet Connection\nResult: Success\n\n$STATUS_REPORT"
        )
        assertThat(result.get("site_url").asString).isEqualTo(SITE_URL)
        assertThat(result.get("app_version").asString).isEqualTo(APP_VERSION)
    }

    @Test
    fun `given status report without diagnostics, when context is built, then troubleshooting results are the report`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val result = contextProvider.buildInitialContext(statusReport = STATUS_REPORT)

        assertThat(result.get("troubleshootingResults").asString).isEqualTo(STATUS_REPORT)
    }

    @Test
    fun `given no selected site, when building status report, then launch address is passed`() = runTest {
        whenever(selectedSite.getIfExists()).thenReturn(null)
        mobileStatusProvider.stub { on { invoke(selectedSite = null, siteAddress = SITE_URL) } doReturn STATUS_REPORT }

        val report = contextProvider.buildStatusReport(siteAddress = SITE_URL)

        assertThat(report).isEqualTo(STATUS_REPORT)
    }

    @Test
    fun `given launch address is the selected site, when building status report, then address is not repeated`() =
        runTest {
            val site = SiteModel().apply { url = SITE_URL }
            whenever(selectedSite.getIfExists()).thenReturn(site)
            mobileStatusProvider.stub { on { invoke(anyOrNull(), anyOrNull()) } doReturn STATUS_REPORT }

            contextProvider.buildStatusReport(siteAddress = SITE_URL)

            verify(mobileStatusProvider).invoke(selectedSite = site, siteAddress = null)
        }

    @Test
    fun `given launch address differs from selected site, when building status report, then address is passed`() =
        runTest {
            val site = SiteModel().apply { url = SITE_URL }
            whenever(selectedSite.getIfExists()).thenReturn(site)
            mobileStatusProvider.stub { on { invoke(anyOrNull(), anyOrNull()) } doReturn STATUS_REPORT }

            contextProvider.buildStatusReport(siteAddress = OTHER_SITE_URL)

            verify(mobileStatusProvider).invoke(selectedSite = site, siteAddress = OTHER_SITE_URL)
        }

    @Test
    fun `given login details with email and token, when building login context, then they are removed`() {
        whenever(unifiedLoginTracker.getFlow()).thenReturn(Flow.LOGIN_SITE_ADDRESS)
        whenever(unifiedLoginTracker.stepBeforeHelp).thenReturn(Step.ENTER_SITE_ADDRESS)
        whenever(unifiedLoginTracker.lastShownError).thenReturn("No account for jane@example.com")
        whenever(unifiedLoginTracker.enteredSiteAddress).thenReturn("https://shop.example/?token=abc")

        val loginContext = contextProvider.buildLoginContext()

        assertThat(loginContext).isEqualTo(
            "# Login context\n" +
                "Login flow: login_site_address\n" +
                "Login step: enter_site_address\n" +
                "Site address entered: https://shop.example/?[redacted]\n" +
                "Last error shown: No account for [email]"
        )
    }

    @Test
    fun `given an error with a password and a line break, when building login context, then it is cleaned`() {
        whenever(unifiedLoginTracker.lastShownError).thenReturn("Wrong password=hunter2\nTry again")

        val loginContext = contextProvider.buildLoginContext()

        assertThat(loginContext.lines().last()).isEqualTo("Last error shown: Wrong password=[redacted] Try again")
    }

    @Test
    fun `given no login state, when building login context, then it has only the header`() {
        val loginContext = contextProvider.buildLoginContext()

        assertThat(loginContext).isEqualTo("# Login context")
    }

    @Test
    fun `given login context, when building context, then it goes before the status report`() {
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val context = contextProvider.buildInitialContext(statusReport = STATUS_REPORT, loginContext = LOGIN_CONTEXT)

        assertThat(context["troubleshootingResults"].asString).isEqualTo("$LOGIN_CONTEXT\n\n$STATUS_REPORT")
    }

    @Test
    fun `given recent warnings and errors, when building recent app logs, then they follow the header and notice`() =
        runTest {
            givenLogEntries(
                logEntry(WooLog.LogLevel.d, "Fetch sites"),
                logEntry(WooLog.LogLevel.i, "Tracked: site_picker_continue_tapped"),
                logEntry(WooLog.LogLevel.w, "Checking WooCommerce API version failed for site 1"),
                logEntry(WooLog.LogLevel.e, "Volley error on https://public-api.wordpress.com/rest/v1.1/me")
            )

            val lines = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty().lines()

            assertThat(lines).hasSize(4)
            assertThat(lines[0]).isEqualTo("# Recent app logs")
            assertThat(lines[1]).startsWith("These lines come from the app log")
            assertThat(lines[2]).endsWith("WP w] Checking WooCommerce API version failed for site 1")
            assertThat(lines[3]).endsWith("WP e] Volley error on https://public-api.wordpress.com/rest/v1.1/me")
        }

    @Test
    fun `given an error older than 30 minutes, when building recent app logs, then it is left out`() = runTest {
        givenLogEntries(
            logEntry(WooLog.LogLevel.e, "Old error", minutesAgo = 31),
            logEntry(WooLog.LogLevel.e, "New error", minutesAgo = 29)
        )

        val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty()

        assertThat(logs).contains("New error").doesNotContain("Old error")
    }

    @Test
    fun `given an error read back from the log file, when building recent app logs, then it is included`() =
        runTest {
            val loggedLine = logEntry(WooLog.LogLevel.e, "Error from the file", minutesAgo = 5).toString()
            givenLogEntries(requireNotNull(LogEntry.fromString(loggedLine)))

            val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty()

            assertThat(logs).contains("Error from the file")
        }

    @Test
    fun `given no recent warnings or errors, when building recent app logs, then there are none`() = runTest {
        givenLogEntries(logEntry(WooLog.LogLevel.i, "Fetch sites"))

        val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true)

        assertThat(logs).isNull()
    }

    @Test
    fun `given a stack trace, when building recent app logs, then only its first line is kept`() = runTest {
        givenLogEntries(
            logEntry(
                WooLog.LogLevel.e,
                "StackTrace: com.android.volley.ClientError\n\tat com.android.volley.toolbox.NetworkUtility.run"
            )
        )

        val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty()

        assertThat(logs.lines().last()).endsWith("WP e] StackTrace: com.android.volley.ClientError")
    }

    @Test
    fun `given secrets and emails, when building recent app logs, then they are removed`() = runTest {
        givenLogEntries(
            logEntry(WooLog.LogLevel.w, "Login failed for jane@example.com, password: hunter2"),
            logEntry(WooLog.LogLevel.w, "Request failed: https://shop.example/wp-json/?token=abc123")
        )

        val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty()

        assertThat(logs)
            .contains("Login failed for [email], password: [redacted]")
            .contains("Request failed: https://shop.example/wp-json/?[redacted]")
            .doesNotContain("jane", "hunter2", "abc123")
    }

    @Test
    fun `given a store response excerpt, when building recent app logs that keep excerpts, then it is kept`() =
        runTest {
            givenLogEntries(logEntry(WooLog.LogLevel.w, STORE_RESPONSE_WARNING))

            val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty()

            assertThat(logs).endsWith(STORE_RESPONSE_WARNING)
        }

    @Test
    fun `given a store response excerpt, when building recent app logs without excerpts, then it is removed`() =
        runTest {
            givenLogEntries(logEntry(WooLog.LogLevel.w, STORE_RESPONSE_WARNING))

            val logs = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = false).orEmpty()

            assertThat(logs).endsWith("Unexpected store response: kind=unacceptable_status_code, status=503")
        }

    @Test
    fun `given more than 20 warnings, when building recent app logs, then the newest 20 are kept`() = runTest {
        givenLogEntries(*(1..25).map { logEntry(WooLog.LogLevel.w, "Warning $it") }.toTypedArray())

        val lines = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty().lines().drop(2)

        assertThat(lines).hasSize(20)
        assertThat(lines.first()).endsWith("Warning 6")
        assertThat(lines.last()).endsWith("Warning 25")
    }

    @Test
    fun `given long warnings, when building recent app logs, then lines and the section stay within limits`() =
        runTest {
            val longText = "word ".repeat(200)
            givenLogEntries(*(1..20).map { logEntry(WooLog.LogLevel.w, "Warning $it $longText") }.toTypedArray())

            val lines = contextProvider.buildRecentAppLogs(keepStoreResponseExcerpts = true).orEmpty().lines().drop(2)

            assertThat(lines).allSatisfy { assertThat(it.length).isLessThanOrEqualTo(500) }
            assertThat(lines.joinToString("\n").length).isLessThanOrEqualTo(4000)
            assertThat(lines.last()).contains("Warning 20 ")
        }

    @Test
    fun `given recent app logs, when building context, then they go between the diagnostics and the status report`() {
        val diagnostics = DiagnosticResult(
            issueType = SupportIssueType.OTHER,
            statuses = listOf(DiagnosticStatus(DiagnosticTest.INTERNET_CONNECTION, TestStatus.Passed))
        )
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val context = contextProvider.buildInitialContext(
            diagnosticResult = diagnostics,
            statusReport = STATUS_REPORT,
            loginContext = LOGIN_CONTEXT,
            recentAppLogs = RECENT_APP_LOGS
        )

        assertThat(context["troubleshootingResults"].asString).isEqualTo(
            "$LOGIN_CONTEXT\n\n## 1. Internet Connection\nResult: Success\n\n$RECENT_APP_LOGS\n\n$STATUS_REPORT"
        )
    }

    private suspend fun givenLogEntries(vararg entries: LogEntry) {
        whenever(wooLog.getCurrentLogEntries()).thenReturn(entries.toList())
    }

    private fun logEntry(level: WooLog.LogLevel, text: String, minutesAgo: Long = 1) =
        LogEntry(WooLog.T.WP, level, text, Date(NOW - TimeUnit.MINUTES.toMillis(minutesAgo)))

    private companion object {
        const val LOGIN_CONTEXT = "# Login context\nLogin step: enter_site_address"
        const val APP_VERSION = "1.2.3"
        const val SITE_ID = 20L
        const val LOCAL_SITE_ID = 456
        const val SITE_URL = "https://example.com"
        const val OTHER_SITE_URL = "https://other.example.com"
        const val STATUS_REPORT = "### Mobile Status Report ###"
        const val RECENT_APP_LOGS = "# Recent app logs\nLog line"
        const val NOW = 1_800_000_000_000L
        const val STORE_RESPONSE_WARNING =
            "Unexpected store response: kind=unacceptable_status_code, status=503, excerpt=Maintenance | Soon"
    }
}
