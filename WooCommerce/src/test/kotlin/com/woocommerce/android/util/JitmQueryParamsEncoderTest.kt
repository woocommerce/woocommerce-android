package com.woocommerce.android.util

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.ui.jitm.JitmQueryParamsEncoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URLEncoder

class JitmQueryParamsEncoderTest {
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val deviceInfo: DeviceInfoWrapper = mock()
    private val deviceFeatures: DeviceFeatures = mock()
    private val encoder = JitmQueryParamsEncoder(
        buildConfigWrapper,
        deviceInfo,
        deviceFeatures,
    )

    @Test
    fun `given debug build, when getEncodedQueryParams called, then proper encoded query params returned`() {
        // GIVEN
        whenever(buildConfigWrapper.debug).thenReturn(BuildConfig.DEBUG)
        whenever(deviceInfo.osVersionCode).thenReturn(29)
        whenever(deviceInfo.name).thenReturn("Pixel 3")
        whenever(deviceFeatures.isNFCAvailable()).thenReturn(true)
        whenever(deviceInfo.locale).thenReturn("en_US")

        // WHEN
        val encoderQueryParams = encoder.getEncodedQueryParams()

        // THEN
        assertThat(encoderQueryParams).isEqualTo(
            URLEncoder.encode(
                "build_type=developer&" +
                    "platform=android&version=${BuildConfig.VERSION_NAME}" +
                    "&os_version=29" +
                    "&device=Pixel_3" +
                    "&nfc=true" +
                    "&locale=en_US",
                Charsets.UTF_8.name()
            )
        )
    }

    @Test
    fun `given release build, when getEncodedQueryParams called, then proper encoded query params returned`() {
        // GIVEN
        whenever(buildConfigWrapper.debug).thenReturn(false)
        whenever(deviceInfo.osVersionCode).thenReturn(27)
        whenever(deviceInfo.name).thenReturn("Pixel 2")
        whenever(deviceFeatures.isNFCAvailable()).thenReturn(false)
        whenever(deviceInfo.locale).thenReturn("ru_RU")

        // WHEN
        val encoderQueryParams = encoder.getEncodedQueryParams()

        // THEN
        assertThat(encoderQueryParams).isEqualTo(
            URLEncoder.encode(
                "platform=android" +
                    "&version=${BuildConfig.VERSION_NAME}" +
                    "&os_version=27" +
                    "&device=Pixel_2" +
                    "&nfc=false" +
                    "&locale=ru_RU",
                Charsets.UTF_8.name()
            )
        )
    }
}
