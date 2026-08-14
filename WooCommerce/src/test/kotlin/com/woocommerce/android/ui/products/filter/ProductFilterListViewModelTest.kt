package com.woocommerce.android.ui.products.filter

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.PluginUrls
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.filters.SavedFilter
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.filter.ProductFilterListViewModel.FilterListOptionItemUiModel
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProductFilterListViewModelTest : BaseUnitTest() {
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var productCategoriesRepository: ProductCategoriesRepository
    private lateinit var networkStatus: NetworkStatus
    private lateinit var productFilterListViewModel: ProductFilterListViewModel
    private lateinit var pluginRepository: PluginRepository
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var saveProductFilterToHistory: SaveProductFilterToHistory
    private lateinit var productFilterHistoryMapper: ProductFilterHistoryMapper
    private lateinit var featureFlagRepository: FeatureFlagRepository
    private val siteModel: SiteModel = SiteModel().apply { id = 123 }
    private val selectedSiteMock: SelectedSite = mock {
        on { getIfExists() }.doReturn(siteModel)
    }
    private val notInstalledPlugin = WooPlugin(false, false, null)
    private val installedPlugin = WooPlugin(true, true, "v1.0")

    @Before
    fun setup() {
        resourceProvider = mock()
        productCategoriesRepository = mock()
        networkStatus = mock()
        pluginRepository = mock()
        analyticsTrackerWrapper = mock()
        saveProductFilterToHistory = mock()
        productFilterHistoryMapper = mock()
        featureFlagRepository = mock()
        productFilterListViewModel = ProductFilterListViewModel(
            savedState = ProductFilterListFragmentArgs(
                selectedStockStatus = "instock",
                selectedProductStatus = "published",
                selectedProductType = "any",
                selectedProductCategoryId = "1",
                selectedProductCategoryName = "any",
            ).toSavedStateHandle(),
            resourceProvider = resourceProvider,
            productCategoriesRepository = productCategoriesRepository,
            networkStatus = networkStatus,
            pluginRepository = pluginRepository,
            selectedSite = selectedSiteMock,
            analyticsTracker = analyticsTrackerWrapper,
            saveProductFilterToHistory = saveProductFilterToHistory,
            productFilterHistoryMapper = productFilterHistoryMapper,
            featureFlagRepository = featureFlagRepository
        )

        whenever(resourceProvider.getString(any())).thenReturn("")
    }

    @Test
    fun `when show products is clicked, then the current filter is saved and returned`() {
        productFilterListViewModel.onShowProductsClicked()

        val expected = ProductFilterResult(
            stockStatus = "instock",
            productType = "any",
            productStatus = "published",
            productCategory = "1",
            productCategoryName = "any"
        )
        val captor = argumentCaptor<ProductFilterResult>()
        verify(saveProductFilterToHistory).invoke(captor.capture())
        Assertions.assertThat(captor.firstValue).isEqualTo(expected)

        val event = productFilterListViewModel.event.value
        Assertions.assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
        Assertions.assertThat((event as MultiLiveEvent.Event.ExitWithResult<*>).data).isEqualTo(expected)
    }

    @Test
    fun `when filter history button is clicked, then entry point is tracked and history is opened`() {
        productFilterListViewModel.onFilterHistoryButtonClicked()

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FILTER_HISTORY_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_PRODUCTS)
        )
        Assertions.assertThat(productFilterListViewModel.event.value)
            .isEqualTo(ProductFilterListViewModel.OpenFilterHistory)
    }

    @Test
    fun `when a past filter is selected, then all filter options are repopulated`() {
        val applied = ProductFilterResult(
            stockStatus = "outofstock",
            productType = "simple",
            productStatus = "draft",
            productCategory = "7",
            productCategoryName = "Boots"
        )
        whenever(productFilterHistoryMapper.fromPayload("payload")).thenReturn(applied)

        productFilterListViewModel.onPastFilterSelected(SavedFilter(id = 1, readableString = "r", payload = "payload"))
        productFilterListViewModel.onShowProductsClicked()

        // onShowProductsClicked rebuilds the result from the repopulated map + selectedCategoryName,
        // so an equal result proves every field (incl. the category name) was applied.
        val captor = argumentCaptor<ProductFilterResult>()
        verify(saveProductFilterToHistory).invoke(captor.capture())
        Assertions.assertThat(captor.firstValue).isEqualTo(applied)
    }

    @Test
    fun `given an undecodable past filter, when selected, then the current selection is unchanged`() {
        whenever(productFilterHistoryMapper.fromPayload("bad")).thenReturn(null)

        productFilterListViewModel.onPastFilterSelected(SavedFilter(id = 1, readableString = "r", payload = "bad"))

        Assertions.assertThat(productFilterListViewModel.getFilterString()).isEqualTo("instock, any, published, 1")
    }

    @Test
    fun `when filters are loaded, then display product status filter`() {
        val productFilters = mutableListOf<ProductFilterListViewModel.FilterListItemUiModel>()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters.addAll(it)
        }

        productFilterListViewModel.loadFilters()

        assertTrue(
            productFilters.firstOrNull {
                it.filterItemKey == WCProductStore.ProductFilterOption.STATUS
            } != null
        )
    }

    @Test
    fun `given there is no info about plugins don't display any explore option`() = testBlocking {
        whenever(pluginRepository.getPluginsInfo(any(), any())).thenReturn(
            emptyMap()
        )
        var productFilters: List<ProductFilterListViewModel.FilterListItemUiModel> = emptyList()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters = it
        }

        productFilterListViewModel.loadFilters()

        val productTypeFilter = productFilters.find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!

        val hasAnExploreOption = productTypeFilter.filterOptionListItems.any {
            it is ProductFilterListViewModel.FilterListOptionItemUiModel.ExploreOptionItemUiModel
        }

        assertFalse(hasAnExploreOption)
    }

    @Test
    fun `given subscriptions is not installed display explore options for subscription product types`() = testBlocking {
        whenever(pluginRepository.getPluginsInfo(any(), any())).thenReturn(
            mapOf(
                WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName to notInstalledPlugin,
                WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName to installedPlugin,
                WooCommerceStore.WooPlugin.WOO_COMPOSITE_PRODUCTS.pluginName to installedPlugin
            )
        )
        var productFilters: List<ProductFilterListViewModel.FilterListItemUiModel> = emptyList()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters = it
        }

        productFilterListViewModel.loadFilters()

        val productTypeFilter = productFilters.find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!

        val exploreOptions = productTypeFilter.filterOptionListItems
            .filterIsInstance<ProductFilterListViewModel.FilterListOptionItemUiModel.ExploreOptionItemUiModel>()

        // Size equals 2 Subscription & Variable Subscriptions
        Assertions.assertThat(exploreOptions.size).isEqualTo(2)
        exploreOptions.forEach { option ->
            Assertions.assertThat(option.url).isEqualTo(PluginUrls.SUBSCRIPTIONS_URL)
        }
    }

    @Test
    fun `given bundles is not installed  display explore option for bundles product type`() = testBlocking {
        whenever(pluginRepository.getPluginsInfo(any(), any())).thenReturn(
            mapOf(
                WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName to installedPlugin,
                WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName to notInstalledPlugin,
                WooCommerceStore.WooPlugin.WOO_COMPOSITE_PRODUCTS.pluginName to installedPlugin
            )
        )
        var productFilters: List<ProductFilterListViewModel.FilterListItemUiModel> = emptyList()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters = it
        }

        productFilterListViewModel.loadFilters()

        val productTypeFilter = productFilters.find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!

        val exploreOptions = productTypeFilter.filterOptionListItems
            .filterIsInstance<ProductFilterListViewModel.FilterListOptionItemUiModel.ExploreOptionItemUiModel>()

        // Only one explore option for bundles
        Assertions.assertThat(exploreOptions.size).isEqualTo(1)
        exploreOptions.forEach { option ->
            Assertions.assertThat(option.url).isEqualTo(PluginUrls.BUNDLES_URL)
        }
    }

    @Test
    fun `given composite products is not installed  display explore option for composite product type`() =
        testBlocking {
            whenever(pluginRepository.getPluginsInfo(any(), any())).thenReturn(
                mapOf(
                    WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName to installedPlugin,
                    WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName to installedPlugin,
                    WooCommerceStore.WooPlugin.WOO_COMPOSITE_PRODUCTS.pluginName to notInstalledPlugin
                )
            )
            var productFilters: List<ProductFilterListViewModel.FilterListItemUiModel> = emptyList()
            productFilterListViewModel.filterListItems.observeForever {
                productFilters = it
            }

            productFilterListViewModel.loadFilters()

            val productTypeFilter =
                productFilters.find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!

            val exploreOptions = productTypeFilter.filterOptionListItems
                .filterIsInstance<ProductFilterListViewModel.FilterListOptionItemUiModel.ExploreOptionItemUiModel>()

            // Only one explore option for composite
            Assertions.assertThat(exploreOptions.size).isEqualTo(1)
            exploreOptions.forEach { option ->
                Assertions.assertThat(option.url).isEqualTo(PluginUrls.COMPOSITE_URL)
            }
        }

    @Test
    fun `given all extensions installed then DON'T display explore options`() = testBlocking {
        whenever(pluginRepository.getPluginsInfo(any(), any())).thenReturn(
            mapOf(
                WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName to installedPlugin,
                WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName to installedPlugin,
                WooCommerceStore.WooPlugin.WOO_COMPOSITE_PRODUCTS.pluginName to installedPlugin
            )
        )
        var productFilters: List<ProductFilterListViewModel.FilterListItemUiModel> = emptyList()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters = it
        }

        productFilterListViewModel.loadFilters()

        val productTypeFilter = productFilters.find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!

        val hasAnExploreOption = productTypeFilter.filterOptionListItems.any {
            it is ProductFilterListViewModel.FilterListOptionItemUiModel.ExploreOptionItemUiModel
        }

        assertFalse(hasAnExploreOption)
        val expectedNumberOfAllAvailableFilters = 9
        assertEquals(productTypeFilter.filterOptionListItems.size, expectedNumberOfAllAvailableFilters)
    }

    @Test
    fun `given a explore option is tapped, then track the event`() = testBlocking {
        val exploreOption = ProductFilterListViewModel.FilterListOptionItemUiModel.ExploreOptionItemUiModel(
            filterOptionItemName = ProductType.COMPOSITE.value,
            filterOptionItemValue = ProductType.COMPOSITE.value,
            url = PluginUrls.COMPOSITE_URL
        )

        productFilterListViewModel.loadFilters()
        productFilterListViewModel.onFilterOptionItemSelected(0, exploreOption)

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.PRODUCT_FILTER_LIST_EXPLORE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_TYPE to exploreOption.filterOptionItemValue)
        )
    }

    @Test
    fun `when product filter types are displayed, then DON'T include not filterable product types`() = testBlocking {
        whenever(pluginRepository.getPluginsInfo(any(), any())).thenReturn(emptyMap())

        var productFilters: List<ProductFilterListViewModel.FilterListItemUiModel> = emptyList()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters = it
        }

        productFilterListViewModel.loadFilters()
        val productTypeFilter = productFilters
            .find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!
            .filterOptionListItems
            .filterIsInstance<
                ProductFilterListViewModel.FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel
                >()

        val hasNoFilterableProductTypeOptions = productTypeFilter.any {
            it.filterOptionItemValue == ProductType.VARIATION.value
        }
        assertFalse(hasNoFilterableProductTypeOptions)
    }
}
