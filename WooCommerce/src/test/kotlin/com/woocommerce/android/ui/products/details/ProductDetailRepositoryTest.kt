package com.woocommerce.android.ui.products.details

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGlobalAttributeStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCTaxStore
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE as INVALID_RESPONSE_ORIGINAL

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailRepositoryTest : BaseUnitTest() {
    private val site = SiteModel()
    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val sut = ProductDetailRepository(
        dispatcher = mock<Dispatcher>(),
        productStore = productStore,
        globalAttributeStore = mock<WCGlobalAttributeStore>(),
        selectedSite = selectedSite,
        taxStore = mock<WCTaxStore>(),
        coroutineDispatchers = mock<CoroutineDispatchers>()
    )

    @Test
    fun `given product store duplication succeeds, when duplicating product, then return successful ID`() = testBlocking {
        // GIVEN
        givenProductStoreReturns(WooResult(DUPLICATED_PRODUCT_ID))

        // WHEN
        val result = sut.duplicateProduct(SOURCE_PRODUCT_ID)

        // THEN
        assertThat(result).isEqualTo(Result.success(DUPLICATED_PRODUCT_ID))
    }

    @Test
    fun `given product store duplication fails, when duplicating product, then preserve exact error in failure`() =
        testBlocking {
            // GIVEN
            val expectedError = WooError(
                type = API_ERROR,
                original = NETWORK_ERROR,
                message = "Unable to duplicate product",
                apiErrorCode = "woocommerce_rest_product_invalid_id",
                errorData = mock<JSONObject>()
            )
            givenProductStoreReturns(WooResult(expectedError))

            // WHEN
            val result = sut.duplicateProduct(SOURCE_PRODUCT_ID)

            // THEN
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
            assertThat((result.exceptionOrNull() as WooException).error).isEqualTo(expectedError)
        }

    @Test
    fun `given product store duplication returns no model or error, when duplicating product, then return invalid response failure`() =
        testBlocking {
            // GIVEN
            givenProductStoreReturns(WooResult())

            // WHEN
            val result = sut.duplicateProduct(SOURCE_PRODUCT_ID)

            // THEN
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
            val error = (result.exceptionOrNull() as WooException).error
            assertThat(error.type).isEqualTo(INVALID_RESPONSE)
            assertThat(error.original).isEqualTo(INVALID_RESPONSE_ORIGINAL)
        }

    private suspend fun givenProductStoreReturns(result: WooResult<Long>) {
        whenever(productStore.duplicateProduct(site, SOURCE_PRODUCT_ID)).thenReturn(result)
    }

    private companion object {
        const val SOURCE_PRODUCT_ID = 42L
        const val DUPLICATED_PRODUCT_ID = 84L
    }
}
