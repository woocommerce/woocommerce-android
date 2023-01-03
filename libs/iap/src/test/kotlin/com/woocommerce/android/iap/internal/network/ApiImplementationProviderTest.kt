package com.woocommerce.android.iap.internal.network

import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ApiImplementationProviderTest {

    private val logWrapperMock: IAPLogWrapper = mock()
    private val buildConfigWrapper: ApiImplementationProvider.BuildConfigWrapper = mock()
    private val realMobileApiProvider: (String?) -> IAPMobilePayAPI = mock()

    private val sut = ApiImplementationProvider(buildConfigWrapper)

    @Test
    fun `given sandbox url, when providing api, then real mobile api provider`() {
        val url = "https://example.com"
        whenever(buildConfigWrapper.iapTestingSandboxUrl).thenReturn(url)
        val sandboxApi: IAPMobilePayAPI = mock()
        whenever(realMobileApiProvider.invoke(url)).thenReturn(sandboxApi)

        val api = sut.providerMobilePayAPI(logWrapperMock, realMobileApiProvider)

        assertThat(api).isEqualTo(sandboxApi)
    }

    @Test
    fun `given empty sandbox url and debug, when providing api, then stubbed mobile api provider`() {
        whenever(buildConfigWrapper.iapTestingSandboxUrl).thenReturn("")
        whenever(buildConfigWrapper.isDebug).thenReturn(true)

        val api = sut.providerMobilePayAPI(logWrapperMock, realMobileApiProvider)

        assertThat(api).isInstanceOf(IAPMobilePayAPIStub::class.java)
    }

    @Test
    fun `given empty sandbox url and release, when providing api, then real mobile api provider`() {
        whenever(buildConfigWrapper.iapTestingSandboxUrl).thenReturn("")
        whenever(buildConfigWrapper.isDebug).thenReturn(false)

        val realApi: IAPMobilePayAPI = mock()
        whenever(realMobileApiProvider.invoke(null)).thenReturn(realApi)

        val api = sut.providerMobilePayAPI(logWrapperMock, realMobileApiProvider)

        assertThat(api).isEqualTo(realApi)
    }
}
