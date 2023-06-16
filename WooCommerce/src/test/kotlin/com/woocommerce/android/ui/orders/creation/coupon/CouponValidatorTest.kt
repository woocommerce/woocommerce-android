package com.woocommerce.android.ui.orders.creation.coupon

import com.woocommerce.android.tools.SelectedSite
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
    fun `given entered code, when coupon is found in store, then validator should mark coupon as valid`() = testBlocking {
        whenever(store.searchCoupons(any(), any(), any(), any()))
            .thenReturn(WooResult(generateSampleCouponSearchResponse("abc")))

        val sut = CouponValidator(selectedSite, store)

        assertThat(sut.isCouponValid("abc")).isTrue
    }

    @Test
    fun `given entered code, when coupon is not found in store, then validator should mark coupon as invalid`() = testBlocking {
        whenever(store.searchCoupons(any(), any(), any(), any()))
            .thenReturn(WooResult())

        val sut = CouponValidator(selectedSite, store)

        assertThat(sut.isCouponValid("ABc")).isFalse
    }

    @Test
    fun `given entered code, then validator should not be case sensitive`() = testBlocking {
        whenever(store.searchCoupons(any(), any(), any(), any()))
            .thenReturn(WooResult(generateSampleCouponSearchResponse("abc")))

        val sut = CouponValidator(selectedSite, store)

        assertThat(sut.isCouponValid("ABc")).isTrue
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
