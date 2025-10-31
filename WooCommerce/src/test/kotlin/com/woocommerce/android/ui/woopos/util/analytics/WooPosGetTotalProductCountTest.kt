package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class WooPosGetTotalProductCountTest {
    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers(
        main = Dispatchers.Unconfined,
        computation = Dispatchers.Unconfined,
        io = Dispatchers.Unconfined
    )
    private val currentTime: WooPosGetTotalProductCount.CurrentTimeProvider = mock()
    private val site: SiteModel = mock()

    private lateinit var sut: WooPosGetTotalProductCount

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(site)

        sut = WooPosGetTotalProductCount(
            productStore = productStore,
            selectedSite = selectedSite,
            currentTime = currentTime,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `given successful response, when invoke called, then returns count`() = runTest {
        // GIVEN
        val expectedCount = 42L
        val successResult = WooResult(model = expectedCount)
        whenever(productStore.fetchProductsCount(any())).thenReturn(successResult)

        // WHEN
        val result = sut.invoke()

        // THEN
        assertEquals(expectedCount.toInt(), result)
    }

    @Test
    fun `given error response, when invoke called, then returns null`() = runTest {
        // GIVEN
        val error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN,
            message = "Error fetching product count"
        )
        val errorResult = WooResult<Long>(error)
        whenever(productStore.fetchProductsCount(any())).thenReturn(errorResult)

        // WHEN
        val result = sut.invoke()

        // THEN
        assertNull(result)
    }

    @Test
    fun `given cached result exists, when invoke called again, then does not fetch from network`() = runTest {
        // GIVEN
        val expectedCount = 42L
        val successResult = WooResult(model = expectedCount)
        whenever(productStore.fetchProductsCount(any())).thenReturn(successResult)

        // WHEN
        sut.invoke()
        sut.invoke()

        // THEN
        verify(productStore, times(1)).fetchProductsCount(any())
    }

    @Test
    fun `given cached result expired, when invoke called, then fetches from network again`() = runTest {
        // GIVEN
        whenever(currentTime()).thenReturn(0L)
        val firstCount = 42L
        val secondCount = 100L
        whenever(productStore.fetchProductsCount(any()))
            .thenReturn(WooResult(model = firstCount))
            .thenReturn(WooResult(model = secondCount))

        // WHEN
        val firstResult = sut.invoke()
        whenever(currentTime()).thenReturn(60 * 60 * 1000L + 1L)
        val secondResult = sut.invoke()

        // THEN
        assertEquals(firstCount.toInt(), firstResult)
        assertEquals(secondCount.toInt(), secondResult)
        verify(productStore, times(2)).fetchProductsCount(any())
    }

    @Test
    fun `given cached result is not expired, when invoke called, then fetches from network once`() = runTest {
        // GIVEN
        whenever(currentTime()).thenReturn(0L)
        val firstCount = 42L
        val secondCount = 100L
        whenever(productStore.fetchProductsCount(any()))
            .thenReturn(WooResult(model = firstCount))
            .thenReturn(WooResult(model = secondCount))

        // WHEN
        val firstResult = sut.invoke()
        whenever(currentTime()).thenReturn(60 * 60 * 1000L - 1L)
        val secondResult = sut.invoke()

        // THEN
        assertEquals(firstCount.toInt(), firstResult)
        assertEquals(firstCount.toInt(), secondResult)
        verify(productStore, times(1)).fetchProductsCount(any())
    }
}
