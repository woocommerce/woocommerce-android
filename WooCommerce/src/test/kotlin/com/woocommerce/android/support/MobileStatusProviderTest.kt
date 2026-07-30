package com.woocommerce.android.support

import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
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

    @Test
    fun `when the report is generated, then it contains all the expected sections`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains(
            MobileStatusProvider.REPORT_HEADING,
            MobileStatusProvider.HEADING_APP,
            MobileStatusProvider.HEADING_DEVICE,
            MobileStatusProvider.HEADING_CONNECTIVITY
        )
    }

    @Test
    fun `when the report is generated, then every section heading states its scope`() = testBlocking {
        val report = sut(SiteModel())

        val headings = report.lines().filter { it.startsWith("## ") }
        assertThat(headings).isNotEmpty
        assertThat(headings).allMatch { it.endsWith(MobileStatusProvider.SCOPE_APP_WIDE) }
    }

    @Test
    fun `when the report is generated, then device and OS information is included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Model: Google Pixel 8")
        assertThat(report).contains("OS: Android 15 (API 35)")
        assertThat(report).contains("Screen: 411x914 dp")
        assertThat(report).contains("Version: 21.3 (2103003)")
    }

    @Test
    fun `when the report is generated, then the device locale and app language are reported separately`() =
        testBlocking {
            val report = sut(SiteModel())

            assertThat(report).contains("Device locale: en-US")
            assertThat(report).contains("App language: en-GB")
        }

    @Test
    fun `when the report is generated, then network information is split into separate lines`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Network Type: WiFi")
        assertThat(report).contains("Carrier: Test Carrier")
        assertThat(report).contains("Country Code: GB")
    }

    @Test
    fun `given no selected site, when the report is generated, then it is still produced`() = testBlocking {
        val report = sut(null)

        assertThat(report).contains(MobileStatusProvider.HEADING_DEVICE)
        assertThat(report).contains("Model: Google Pixel 8")
    }

    @Test
    fun `given a section fails, when the report is generated, then the other sections are still produced`() =
        testBlocking {
            deviceInfo.stub { on { name } doThrow RuntimeException("boom") }

            val report = sut(SiteModel())

            assertThat(report).contains(MobileStatusProvider.SECTION_UNAVAILABLE)
            assertThat(report).contains("Version: 21.3 (2103003)")
            assertThat(report).contains("Network Type: WiFi")
        }
}
