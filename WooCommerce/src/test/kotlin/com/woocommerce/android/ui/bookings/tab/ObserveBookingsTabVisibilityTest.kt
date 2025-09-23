package com.woocommerce.android.ui.bookings.tab

import app.cash.turbine.test
import com.woocommerce.android.ui.bookings.tab.ObserveBookingsTabVisibility.Companion.BOOKING_PRODUCT_TYPE
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBookingsTabVisibilityTest : BaseUnitTest() {
    private val testScope = TestScope(coroutinesTestRule.testDispatcher)

    private val bookableProdsFilterOptions = mapOf(
        ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
        ProductFilterOption.TYPE to BOOKING_PRODUCT_TYPE
    )
    private val bookableProdsCountFlow = MutableStateFlow(0L)

    private val productListRepository: ProductListRepository = mock {
        on {
            observeProductsCount(
                filterOptions = bookableProdsFilterOptions,
                excludeSampleProducts = true
            )
        }.thenReturn(bookableProdsCountFlow)
    }

    private lateinit var sut: ObserveBookingsTabVisibility

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        sut = ObserveBookingsTabVisibility(
            productListRepository,
            appCoroutineScope = testScope
        )
    }

    @Test
    fun `when invoke is called, then bookable products are fetched`() = testBlocking {
        bookableProdsCountFlow.value = 2

        setup()

        sut(ciabSite()).test {
            verify(productListRepository).fetchProductList(
                loadMore = false,
                productFilterOptions = mapOf(ProductFilterOption.TYPE to BOOKING_PRODUCT_TYPE),
                excludedProductIds = emptyList(),
                sortType = null
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given CIAB site and bookings products published, when invoke, then emits true`() = testBlocking {
        bookableProdsCountFlow.value = 1

        setup()

        sut(ciabSite()).test {
            val showBookingTabValue = awaitItem()
            assertTrue(showBookingTabValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given CIAB site with zero booking products, when invoke, then emits false`() = testBlocking {
        bookableProdsCountFlow.value = 0

        setup()

        sut(ciabSite()).test {
            val showBookingTabValue = awaitItem()
            assertFalse(showBookingTabValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given non-CIAB site, when invoke, then emits false`() = testBlocking {
        bookableProdsCountFlow.value = 10

        setup()

        sut(nonCiabSite()).test {
            val showBookingTabValue = awaitItem()
            assertFalse(showBookingTabValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given non-Commerce Garden CIAB site, when invoke, then emits false`() = testBlocking {
        bookableProdsCountFlow.value = 10

        setup()

        sut(nonCommerceGardenSite()).test {
            val showBookingTabValue = awaitItem()
            assertFalse(showBookingTabValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given same inputs produce same result, when values update, then only emits once`() = testBlocking {
        bookableProdsCountFlow.value = 2

        setup()

        sut(ciabSite()).test {
            val firstEmission = awaitItem()
            assertTrue(firstEmission)
            // When inputs change but computed value remains true
            bookableProdsCountFlow.value = 3 // still true
            // Then no new emissions due to distinctUntilChanged
            expectNoEvents()

            // Now change to false
            bookableProdsCountFlow.value = 0
            val secondEmission = awaitItem()
            assertFalse(secondEmission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given bookable products fetch fails onStart, when invoke, then emits based on persisted values`() =
        testBlocking {
            bookableProdsCountFlow.value = 1

            setup()

            sut(ciabSite()).test {
                assert(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun ciabSite(): SiteModel = SiteModel().apply {
        setIsGardenSite(true)
        gardenName = "commerce"
    }

    private fun nonCiabSite(): SiteModel = SiteModel().apply {
        setIsGardenSite(false)
        gardenName = "commerce"
    }

    private fun nonCommerceGardenSite(): SiteModel = SiteModel().apply {
        setIsGardenSite(true)
        gardenName = "other"
    }
}
