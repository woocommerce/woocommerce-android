package com.woocommerce.android.support

import android.content.Context
import android.content.pm.PackageManager
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MobileStatusProviderTest : BaseUnitTest() {
    private val envDataSource: ZendeskEnvironmentDataSource = mock {
        on { generateVersionName(any()) } doReturn "21.3"
        on { generateVersionCode(any()) } doReturn 2103003
        on { totalAvailableMemorySize } doReturn "12.4 GB"
        on { generateNetworkInformation(any()) } doReturn
            "Network Type: WiFi\nCarrier: Test Carrier\nCountry Code: GB"
    }

    private val deviceInfo: DeviceInfoWrapper = mock {
        on { name } doReturn "Google Pixel 8"
        on { osName } doReturn "15"
        on { osVersionCode } doReturn 35
        on { screenWidthDp } doReturn 411
        on { screenHeightDp } doReturn 914
        on { localeTag } doReturn "en-US"
    }

    private val localeProvider: LocaleProvider = mock {
        on { provideLocale() } doReturn Locale.UK
    }

    private val sut = MobileStatusProvider(
        context = mock(),
        envDataSource = envDataSource,
        deviceInfo = deviceInfo,
        localeProvider = localeProvider
    )

    /**
     * The whole report in one assertion. Field-by-field `contains` checks cannot see a field that silently
     * disappears, a section that lands in the wrong order, or a stray blank line; this does, and it doubles as
     * the readable example of what the report looks like. The tests below cover only what this fixture cannot
     * express — branches, and values it has no second combination for.
     */
    @Test
    fun `when the report is generated, then it matches the expected report`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report.trimEnd()).isEqualTo(EXPECTED_REPORT)
    }

    @Test
    fun `given a section fails, when the report is generated, then only that section degrades`() = testBlocking {
        deviceInfo.stub { on { name } doThrow RuntimeException("boom") }

        val report = sut(SiteModel())

        assertThat(report.section("## Device")).isEqualTo("Info not found")
        assertThat(report).contains("Version: 21.3 (2103003)")
        assertThat(report).contains("Network Type: WiFi")
    }

    @Test
    fun `given the app was installed from Play, when the report is generated, then the installer is reported`() =
        testBlocking {
            val report = providerWithInstaller { PLAY_STORE }(SiteModel())

            assertThat(report).contains("Install source: $PLAY_STORE")
        }

    @Test
    fun `given no installer of record, when the report is generated, then the app is reported as sideloaded`() =
        testBlocking {
            val report = providerWithInstaller { null }(SiteModel())

            assertThat(report).contains("Install source: sideloaded")
        }

    @Test
    fun `given the installer lookup fails, when the report is generated, then the source is unknown`() =
        testBlocking {
            val report = providerWithInstaller { throw IllegalArgumentException("nope") }(SiteModel())

            assertThat(report).contains("Install source: unknown")
        }

    /** The lines of one section, without its heading, so a section can be asserted on as a whole. */
    private fun String.section(heading: String) =
        substringAfter("$heading\n").substringBefore("\n\n").trim()

    /**
     * Unit tests run with `Build.VERSION.SDK_INT` of 0, so the pre-API-30 `getInstallerPackageName` branch is the
     * one exercised here.
     */
    @Suppress("DEPRECATION")
    private fun providerWithInstaller(installer: () -> String?): MobileStatusProvider {
        val packageManager = mock<PackageManager> {
            on { getInstallerPackageName(PACKAGE_NAME) } doAnswer { installer() }
        }
        return MobileStatusProvider(
            context = mock<Context> {
                on { this.packageManager } doReturn packageManager
                on { packageName } doReturn PACKAGE_NAME
            },
            envDataSource = envDataSource,
            deviceInfo = deviceInfo,
            localeProvider = localeProvider
        )
    }

    private companion object {
        const val PLAY_STORE = "com.android.vending"
        const val PACKAGE_NAME = "com.woocommerce.android"

        private val EXPECTED_REPORT = """
            ### Mobile Status Report generated via the WooCommerce Android app ###

            ## App
            Version: 21.3 (2103003)
            Build: wasabi / debug
            Install source: unknown

            ## Device
            Model: Google Pixel 8
            OS: Android 15 (API 35)
            Free space: 12.4 GB
            Screen: 411x914 dp
            Device locale: en-US
            App language: en-GB

            ## Connectivity
            Network Type: WiFi
            Carrier: Test Carrier
            Country Code: GB
        """.trimIndent().trim()
    }
}
