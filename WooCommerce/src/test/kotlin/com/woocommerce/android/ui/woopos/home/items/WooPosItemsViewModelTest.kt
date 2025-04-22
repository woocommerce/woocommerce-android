package com.woocommerce.android.ui.woopos.home.items

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooPosItemsViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val tabs = listOf(
        WooPosItemsViewState.Tab(
            R.string.woopos_products_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsViewState.Tab(
            R.string.woopos_coupons_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Normal
        )
    )

    private val productsDataSource: WooPosProductsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val posPreferencesRepository: WooPosPreferencesRepository = mock()
    private val wooPosItemsNavigator: WooPosItemsNavigator = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        onBlocking { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
    }
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val isProductsSearchEnabled: WooPosIsProductsSearchEnabled = mock()
    private val searchHelper: WooPosItemsSearchHelper = mock()
    private val tabsHelper: WooPosItemsTabsHelper = mock {
        on { defaultTabs }.thenReturn(tabs)
    }

    @Before
    fun setup() {
        whenever(posPreferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser).thenReturn(
            flowOf(false)
        )

        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple",
                isDownloadable = false,
            ),
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )

        whenever(searchHelper.getInitialSearchState(any())).thenReturn(
            WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )
    }

    @Test
    fun `given variations screen, when clicked back, then trigger proper event`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "variable",
                isVariable = true
            )
        )
        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.BackButtonClicked)

        verify(wooPosItemsNavigator).sendNavigationEvent(
            WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
        )
    }



    @Test
    fun `given products search feature enabled, when view model created, then search state is visible`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(isProductsSearchEnabled()).thenReturn(true)
        whenever(searchHelper.getInitialSearchState(true)).thenReturn(
            WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.Content
            assertThat(contentState.search).isInstanceOf(WooPosItemsViewState.SearchState.Visible::class.java)
            val searchState = contentState.search as WooPosItemsViewState.SearchState.Visible
            assertThat(searchState.state).isEqualTo(WooPosSearchInputState.Closed)
        }
    }

    @Test
    fun `given products search feature disabled, when view model created, then search state is hidden`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(isProductsSearchEnabled()).thenReturn(false)
        whenever(searchHelper.getInitialSearchState(false)).thenReturn(
            WooPosItemsViewState.SearchState.Hidden
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.Content
            assertThat(contentState.search).isInstanceOf(WooPosItemsViewState.SearchState.Hidden::class.java)
        }
    }

    @Test
    fun `given search visible, when close search clicked, then search state is closed`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(isProductsSearchEnabled()).thenReturn(true)

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        // THEN
        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `given search visible, when search text changed, then search helper is called`() = runTest {
        val query = "test query"
        val viewModel = createViewModel()

        viewModel.onUIEvent(WooPosItemsUIEvent.SearchChanged(query, 0))

        verify(searchHelper).onSearchChanged(query, 0)
    }

    @Test
    fun `given search visible, when clear search clicked, then search helper is called`() = runTest {
        val viewModel = createViewModel()

        viewModel.onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)

        verify(searchHelper).onClearSearchClicked()
    }

    @Test
    fun `given search visible, when close search clicked, then search helper is called`() = runTest {
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )
        whenever(isProductsSearchEnabled()).thenReturn(true)

        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `when tab clicked, then tab is selected and state is updated`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            )
        )

        whenever(productsDataSource.loadProducts(any())).thenReturn(
            flowOf(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.success(products)
                )
            )
        )

        val couponsTab = WooPosItemsViewState.Tab(
            R.string.woopos_coupons_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Normal
        )

        whenever(tabsHelper.selectTab(any(), eq(couponsTab))).thenReturn(
            listOf(
                WooPosItemsViewState.Tab(
                    R.string.woopos_products_screen_title,
                    WooPosItemsViewState.Tab.HighlightLevel.Normal
                ),
                WooPosItemsViewState.Tab(
                    R.string.woopos_coupons_screen_title,
                    WooPosItemsViewState.Tab.HighlightLevel.Full
                )
            )
        )

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // THEN
        viewModel.viewState.test {
            val value = awaitItem() as WooPosItemsViewState.Content
            assertThat(value.contentType).isEqualTo(WooPosItemsViewState.Content.ContentState.CouponsList)
        }
    }

    private fun createViewModel() =
        WooPosItemsViewModel(
            productsDataSource,
            fromChildToParentEventSender,
            priceFormat,
            posPreferencesRepository,
            wooPosItemsNavigator,
            analyticsTracker,
            searchHelper,
            isProductsSearchEnabled,
            tabsHelper,
        )
}
