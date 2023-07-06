package com.woocommerce.android.ui.orders.creation.coupon

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.coupon.edit.CouponValidator
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.store.CouponStore

@OptIn(ExperimentalCoroutinesApi::class)
class CouponValidatorTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val store: CouponStore = mock()


    @Test
    fun `given entered code, when coupon is found in store, then validator should mark coupon as valid`() =
        testBlocking {
            val couponValidationResult = CouponValidator.CouponValidationResult.VALID
            whenever(store.searchCoupons(any(), any(), any(), any()))
                .thenReturn(WooResult(generateSampleCouponSearchResponse("abc")))

            val sut = CouponValidator(selectedSite, store)

            assertThat(sut.isCouponValid("abc")).isEqualTo(couponValidationResult)
        }

    @Test
    fun `given entered code, when coupon is not found in store, then validator should mark coupon as invalid`() =
        testBlocking {
            whenever(store.searchCoupons(any(), any(), any(), any()))
                .thenReturn(WooResult())

            val sut = CouponValidator(selectedSite, store)

            assertThat(sut.isCouponValid("ABc")).isEqualTo(CouponValidator.CouponValidationResult.INVALID)
        }

    @Test
    fun `given entered code, then validator should not be case sensitive`() = testBlocking {
        whenever(store.searchCoupons(any(), any(), any(), any()))
            .thenReturn(WooResult(generateSampleCouponSearchResponse("abc")))

        val sut = CouponValidator(selectedSite, store)

        assertThat(sut.isCouponValid("ABc")).isEqualTo(CouponValidator.CouponValidationResult.VALID)
    }

    @Test
    fun `given entered code, when backend returns error, then validator should return networkerror`() = testBlocking {
        whenever(store.searchCoupons(any(), any(), any(), any()))
            .thenReturn(
                WooResult(
                    error = WooError(
                        WooErrorType.GENERIC_ERROR,
                        BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

        val sut = CouponValidator(selectedSite, store)
        assertThat(sut.isCouponValid("abc")).isEqualTo(CouponValidator.CouponValidationResult.NETWORK_ERROR)
    }

    private fun generateSampleCouponSearchResponse(couponCode: String) =
        CouponStore.CouponSearchResult(
            coupons = listOf(
                CouponWithEmails(
                    coupon = CouponEntity(
                        code = couponCode,
                        id = LocalOrRemoteId.RemoteId(1L),
                        localSiteId = LocalOrRemoteId.LocalId(1)
                    ),
                    listOf()
                )

            ),
            canLoadMore = true
        )
}
