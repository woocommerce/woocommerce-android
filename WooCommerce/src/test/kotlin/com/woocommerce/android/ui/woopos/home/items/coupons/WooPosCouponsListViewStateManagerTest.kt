package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.util.GetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
class WooPosCouponsListViewStateManagerTest {

    @JvmField
    @Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val formatCouponSummary: WooPosFormatCouponSummary = mock()
    private val getCachedStoreCurrency: GetCachedStoreCurrency = mock()
    private val couponsDataSource: WooPosCouponsDataSource = mock {
        onBlocking { couponsFlow }.thenReturn(MutableStateFlow(emptyList()))
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }

    private val sat = WooPosCouponsListViewStateManager(couponsDataSource, formatCouponSummary, getCachedStoreCurrency)

    @Before
    fun setup() {
        wheneverBlocking { getCachedStoreCurrency() }.thenReturn("USD")
        whenever(formatCouponSummary.invoke(anyOrNull(), anyOrNull())).thenAnswer { "" }
    }

    @Test
    fun `given empty cache, when collecting viewState, then emits Empty`() = runTest {
        // GIVEN
        whenever(couponsDataSource.couponsFlow).thenReturn(MutableStateFlow(emptyList()))

        // WHEN
        val state = sat.viewState.take(1).last()

        // THEN
        assertThat(state).isInstanceOf(WooPosCouponsViewState.Error::class.java)
    }

    @Test
    fun `given empty cache, when fetching first page, then emits Loading`() = runTest {
        // GIVEN
        whenever(couponsDataSource.couponsFlow).thenReturn(MutableStateFlow(emptyList()))
        var currentState: WooPosCouponsViewState = WooPosCouponsViewState.Empty()
        launch {
            sat.viewState.collect {
                currentState = it
            }
        }

        // WHEN
        sat.fetchCoupons(this)

        // THEN
        assertThat(currentState).isInstanceOf(WooPosCouponsViewState.Loading::class.java)
    }

    @Test
    fun `given non-empty cache and successful initial fetch, when fetchCoupons, then emits Content→Loading→Content(pagination=Loading)`() =
        runTest {

        }

    @Test
    fun `given non-empty cache and failed initial fetch, when fetchCoupons, then emits Content→Loading→Error`() =
        runTest {

        }

    @Test
    fun `given initial fetch succeeded (canLoadMore), when loadMore fails, then emits pagination‐error`() =
        runTest {

        }
}
