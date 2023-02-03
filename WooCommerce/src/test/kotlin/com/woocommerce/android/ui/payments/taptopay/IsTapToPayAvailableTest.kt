package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsTapToPayAvailableTest {

    private val systemVersionUtilsWrapper = mock<SystemVersionUtilsWrapper> {
        on { isAtLeastP() }.thenReturn(true)
    }
    private val isTapToPayEnabled: IsTapToPayEnabled = mock()

    @Test
    fun `given device has no NFC, then tap to pay is not available`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(false)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(isTapToPayEnabled.invoke()).thenReturn(true)

        val result = IsTapToPayAvailable(deviceFeatures, systemVersionUtilsWrapper).invoke(
            "US",
            isTapToPayEnabled
        )

        assertFalse(result)
    }

    @Test
    fun `given device has no Google Play Services, then tap to pay is not available`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(false)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(isTapToPayEnabled.invoke()).thenReturn(true)

        val result = IsTapToPayAvailable(deviceFeatures, systemVersionUtilsWrapper).invoke(
            "US",
            isTapToPayEnabled
        )

        assertFalse(result)
    }

    @Test
    fun `given device has os less than Android 9, then tap to pay is not available`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(false)
        whenever(isTapToPayEnabled.invoke()).thenReturn(true)

        val result = IsTapToPayAvailable(context, systemVersionUtilsWrapper).invoke(
            "US",
            isTapToPayEnabled
        )

        assertFalse(result)
    }

    @Test
    fun `given country other than US, then tap to pay is not available`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(isTapToPayEnabled.invoke()).thenReturn(true)

        val result = IsTapToPayAvailable(context, systemVersionUtilsWrapper).invoke(
            "CA",
            isTapToPayEnabled
        )

        assertFalse(result)
    }

    @Test
    fun `given tap to pay feature flag is not enabled, then tap to pay is not available`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(isTapToPayEnabled.invoke()).thenReturn(false)

        val result = IsTapToPayAvailable(context, systemVersionUtilsWrapper).invoke(
            "US",
            isTapToPayEnabled
        )

        assertFalse(result)
    }

    @Test
    fun `given device satisfies all the requirements, then tap to pay is available`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(isTapToPayEnabled.invoke()).thenReturn(true)

        val result = IsTapToPayAvailable(context, systemVersionUtilsWrapper).invoke(
            "US",
            isTapToPayEnabled
        )

        assertTrue(result)
    }
}
