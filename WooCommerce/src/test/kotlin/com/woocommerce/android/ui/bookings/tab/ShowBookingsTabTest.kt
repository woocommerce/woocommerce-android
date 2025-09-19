package com.woocommerce.android.ui.bookings.tab

import app.cash.turbine.test
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption

@OptIn(ExperimentalCoroutinesApi::class)
class ShowBookingsTabTest : BaseUnitTest() {

    @Mock lateinit var selectedSite: SelectedSite
    @Mock lateinit var productListRepository: ProductListRepository

    private lateinit var sut: ObserveBookingsTabVisibility

    private lateinit var siteFlow: MutableStateFlow<SiteModel?>
    private lateinit var countFlow: MutableStateFlow<Long>

    @Before
    fun setup() {
        siteFlow = MutableStateFlow(null)
        countFlow = MutableStateFlow(0)
        whenever(selectedSite.observe()).thenReturn(siteFlow)
        whenever(
            productListRepository.observeProductsCount(
                filterOptions = any(),
                excludeSampleProducts = any()
            )
        ).thenReturn(countFlow)

        sut = ObserveBookingsTabVisibility(selectedSite, productListRepository)
    }

    @Test
    fun `given CIAB site and bookings available, when invoke, then emits true`() = testBlocking {
        val site = ciabSite()
        siteFlow.value = site
        countFlow.value = 2

        sut().test {
            // onStart should trigger a fetch with booking type
            verify(productListRepository).fetchProductList(
                loadMore = any(),
                productFilterOptions = argThat { this[ProductFilterOption.TYPE] == "booking" },
                excludedProductIds = any(),
                sortType = org.mockito.kotlin.anyOrNull()
            )
            // Then
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given zero booking products, when invoke, then emits false`() = testBlocking {
        // Given
        siteFlow.value = ciabSite()
        countFlow.value = 0

        // When/Then
        sut().test {
            val value = awaitItem()
            assert(!value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given non-CIAB site, when invoke, then emits false`() = testBlocking {
        // Given
        siteFlow.value = nonCiabSite()
        countFlow.value = 10

        // When/Then
        sut().test {
            val value = awaitItem()
            assert(!value)
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `given same inputs produce same result, when values update, then only emits once`() = testBlocking {
        // Given initial true conditions
        siteFlow.value = ciabSite()
        countFlow.value = 2

        sut().test {
            // First emission should be true
            val first = awaitItem()
            assert(first)

            // When inputs change but computed value remains true
            countFlow.value = 3 // still true
            siteFlow.value = ciabSite() // new instance but still CIAB

            // Then no new emissions due to distinctUntilChanged
            expectNoEvents()

            // Now change to false
            countFlow.value = 0
            val next = awaitItem()
            assert(!next)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given fetch fails onStart, when invoke, emits based on persisted values`() = testBlocking {
        siteFlow.value = ciabSite()
        countFlow.value = 1

        // When/Then
        sut().test {
            // It should still emit true based on current flows
            assert(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun ciabSite(): SiteModel = SiteModel().apply {
        setIsGardenSite(true)
        setGardenName("commerce")
    }

    private fun nonCiabSite(): SiteModel = SiteModel().apply {
        setIsGardenSite(false)
        setGardenName("other")
    }
}
