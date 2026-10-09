package com.woocommerce.android.ui.jitm

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class JitmBannerAdapterTest : BaseUnitTest() {
    private val jitmStoreInMemoryCache: JitmStoreInMemoryCache = mock()

    private val sut = JitmBannerAdapter(jitmStoreInMemoryCache)

    @Test
    fun `given dismiss fails with error, when dismissMessage, then error is returned unchanged`() = testBlocking {
        val error = WooError(
            type = WooErrorType.API_ERROR,
            original = GenericErrorType.SERVER_ERROR,
            message = "test error",
            apiErrorCode = "rest_invalid_signature"
        )
        whenever(jitmStoreInMemoryCache.dismissJitmMessage(MESSAGE_PATH, JITM_ID, FEATURE_CLASS))
            .thenReturn(WooResult(error))

        val result = sut.dismissMessage(MESSAGE_PATH, JITM_ID, FEATURE_CLASS)

        assertThat(result.isError).isTrue()
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `given dismiss succeeds, when dismissMessage, then success is returned`() = testBlocking {
        whenever(jitmStoreInMemoryCache.dismissJitmMessage(MESSAGE_PATH, JITM_ID, FEATURE_CLASS))
            .thenReturn(WooResult(true))

        val result = sut.dismissMessage(MESSAGE_PATH, JITM_ID, FEATURE_CLASS)

        assertThat(result.model).isTrue()
    }

    private companion object {
        const val MESSAGE_PATH = "woomobile:my_store:admin_notices"
        const val JITM_ID = "12345"
        const val FEATURE_CLASS = "woomobile_ipp"
    }
}
