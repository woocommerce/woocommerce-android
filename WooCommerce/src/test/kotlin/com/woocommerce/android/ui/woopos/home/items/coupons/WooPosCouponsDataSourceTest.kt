package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponListHandler
import com.woocommerce.android.ui.woopos.home.items.common.FetchOptions
import com.woocommerce.android.ui.woopos.home.items.common.FetchResult
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosCouponsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private lateinit var dataSource: WooPosCouponsDataSource
    private val handler: CouponListHandler = mock()

    private val sampleCoupons = listOf(
        Coupon(
            id = 1,
            code = "DISCOUNT10",
            productIds = emptyList(),
            categoryIds = emptyList(),
            restrictions = Coupon.CouponRestrictions(
                excludedProductIds = emptyList(),
                excludedCategoryIds = emptyList(),
                restrictedEmails = emptyList()
            )
        ),
        Coupon(
            id = 2,
            code = "WELCOME5",
            productIds = emptyList(),
            categoryIds = emptyList(),
            restrictions = Coupon.CouponRestrictions(
                excludedProductIds = emptyList(),
                excludedCategoryIds = emptyList(),
                restrictedEmails = emptyList()
            )
        )
    )

    private val moreCoupons = listOf(
        Coupon(
            id = 3,
            code = "SUMMER15",
            productIds = emptyList(),
            categoryIds = emptyList(),
            restrictions = Coupon.CouponRestrictions(
                excludedProductIds = emptyList(),
                excludedCategoryIds = emptyList(),
                restrictedEmails = emptyList()
            )
        ),
        Coupon(
            id = 4,
            code = "NEWYEAR",
            productIds = emptyList(),
            categoryIds = emptyList(),
            restrictions = Coupon.CouponRestrictions(
                excludedProductIds = emptyList(),
                excludedCategoryIds = emptyList(),
                restrictedEmails = emptyList()
            )
        )
    )

    @Test
    fun `given force refresh, when fetchFromRemote called, then should clear cache`() = runTest {
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(handler.couponsFlow).thenReturn(flowOf(sampleCoupons))

        val sut = WooPosCouponsDataSource(handler)

        sut.fetchData(FetchOptions(forceRefresh = false)).first()
        assertThat(sut.fetchData(FetchOptions(forceRefresh = false)).first()).isInstanceOf(FetchResult.Cached::class.java)

        sut.fetchData(FetchOptions(forceRefresh = true)).first()

        val result = sut.fetchData(FetchOptions(forceRefresh = false)).first()
        assertThat(result).isInstanceOf(FetchResult.Cached::class.java)
        val cachedResult = result as FetchResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(sampleCoupons)
    }

    @Test
    fun `given cached coupons, when fetchData called, then should emit cached coupons first`() = runTest {
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(handler.couponsFlow).thenReturn(flowOf(sampleCoupons))

        val sut = WooPosCouponsDataSource(handler)

        sut.fetchData(FetchOptions(forceRefresh = false)).first()
        val result = sut.fetchData(FetchOptions(forceRefresh = false)).first()

        assertThat(result).isInstanceOf(FetchResult.Cached::class.java)
        val cachedResult = result as FetchResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(sampleCoupons)
    }

    @Test
    fun `given cached and remote coupons, when fetchData called, then should emit remote coupons after cached`() = runTest {
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(handler.couponsFlow).thenReturn(flowOf(sampleCoupons))

        val sut = WooPosCouponsDataSource(handler)

        sut.fetchData(FetchOptions(forceRefresh = false)).first()
        val result = sut.fetchData(FetchOptions(forceRefresh = false)).toList()

        val cachedResult = result[0] as FetchResult.Cached
        val remoteResult = result[1] as FetchResult.Remote

        assertThat(cachedResult.data).containsExactlyElementsOf(sampleCoupons)
        assertThat(remoteResult.result.isSuccess).isTrue()
        assertThat(remoteResult.result.getOrNull()).containsExactlyElementsOf(sampleCoupons)
    }

    @Test
    fun `given remote fetch fails, when fetchData called, then emit empty cached and error`() = runTest {
        val error = Exception("Remote fetch failed")
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.failure(error))
        whenever(handler.couponsFlow).thenReturn(flowOf(emptyList()))

        val sut = WooPosCouponsDataSource(handler)

        val result = sut.fetchData(FetchOptions(forceRefresh = false)).toList()

        val cachedResult = result[0] as FetchResult.Cached
        val remoteResult = result[1] as FetchResult.Remote

        assertThat(cachedResult.data).isEmpty()
        assertThat(remoteResult.result.isFailure).isTrue()
        assertThat(remoteResult.result.exceptionOrNull()).isEqualTo(error)
    }


    @Test
    fun `given successful loadMore, when loadMore called, then should add coupons to cache and return them`() = runTest {
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(handler.loadMore()).thenReturn(Result.success(Unit))
        whenever(handler.couponsFlow).thenReturn(
            flowOf(sampleCoupons),
            flowOf(sampleCoupons + moreCoupons)
        )

        val sut = WooPosCouponsDataSource(handler)

        sut.fetchData(FetchOptions(forceRefresh = false)).first()

        val result = sut.loadMore()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).containsExactlyElementsOf(sampleCoupons + moreCoupons)

        val cached = sut.fetchData(FetchOptions(forceRefresh = false)).first() as FetchResult.Cached
        assertThat(cached.data).containsExactlyElementsOf(sampleCoupons + moreCoupons)
    }

    @Test
    fun `given failed loadMore, when loadMore called, then return error and cache remains unchanged`() = runTest {
        val exception = Exception("Load more failed")
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(handler.loadMore()).thenReturn(Result.failure(exception))
        whenever(handler.couponsFlow).thenReturn(flowOf(sampleCoupons))

        val sut = WooPosCouponsDataSource(handler)

        sut.fetchData(FetchOptions(forceRefresh = false)).first()

        val result = sut.loadMore()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)

        val cached = sut.fetchData(FetchOptions(forceRefresh = false)).first() as FetchResult.Cached
        assertThat(cached.data).containsExactlyElementsOf(sampleCoupons)
    }

    @Test
    fun `given no cached coupons and remote fetch fails, when fetchData called, then emit empty cache and then error`() = runTest {
        val exception = Exception("Remote failed")
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.failure(exception))
        whenever(handler.couponsFlow).thenReturn(flowOf(emptyList()))

        val sut = WooPosCouponsDataSource(handler)

        val result = sut.fetchData(FetchOptions(forceRefresh = false)).toList()

        val cachedResult = result[0] as FetchResult.Cached
        val remoteResult = result[1] as FetchResult.Remote

        assertThat(cachedResult.data).isEmpty()
        assertThat(remoteResult.result.isFailure).isTrue()
        assertThat(remoteResult.result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `given empty coupon list from handler, when fetchData called, then should emit empty cache and remote result`() = runTest {
        whenever(handler.fetchCoupons(forceRefresh = true)).thenReturn(Result.success(Unit))
        whenever(handler.couponsFlow).thenReturn(flowOf(emptyList()))

        val sut = WooPosCouponsDataSource(handler)

        val result = sut.fetchData(FetchOptions(forceRefresh = false)).toList()

        val cachedResult = result[0] as FetchResult.Cached
        val remoteResult = result[1] as FetchResult.Remote

        assertThat(cachedResult.data).isEmpty()
        assertThat(remoteResult.result.isSuccess).isTrue()
        assertThat(remoteResult.result.getOrNull()).isEmpty()
    }
}
