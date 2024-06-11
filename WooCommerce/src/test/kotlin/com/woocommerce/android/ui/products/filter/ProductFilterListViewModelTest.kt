package com.woocommerce.android.ui.products.filter

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.PluginUrls
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.products.ProductFilterProductRestrictions
import com.woocommerce.android.ui.products.ProductRestriction
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProductFilterListViewModelTest : BaseUnitTest() {
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var productCategoriesRepository: ProductCategoriesRepository
    private lateinit var networkStatus: NetworkStatus
    private lateinit var productRestrictions: ProductFilterProductRestrictions
    private lateinit var productFilterListViewModel: ProductFilterListViewModel
    private lateinit var pluginRepository: PluginRepository
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
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
        productRestrictions = mock()
        pluginRepository = mock()
        analyticsTrackerWrapper = mock()
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
            productRestrictions = productRestrictions,
            pluginRepository = pluginRepository,
            selectedSite = selectedSiteMock,
            analyticsTrackerWrapper
        )

        whenever(resourceProvider.getString(any())).thenReturn("")
    }

    @Test
    fun `given there is no NonPublish product restriction, then display product status filter`() {
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
    fun `given there is a NonPublish product restriction, then do not display product status filter`() {
        val productFilters = mutableListOf<ProductFilterListViewModel.FilterListItemUiModel>()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters.addAll(it)
        }
        whenever(productRestrictions.restrictions).thenReturn(
            listOf(ProductRestriction.NonPublishedProducts)
        )

        productFilterListViewModel.loadFilters()

        assertFalse(
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
    fun `given composite products is not installed  display explore option for composite product type`() = testBlocking {
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

        val productTypeFilter = productFilters.find { it.filterItemKey == WCProductStore.ProductFilterOption.TYPE }!!

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
}
