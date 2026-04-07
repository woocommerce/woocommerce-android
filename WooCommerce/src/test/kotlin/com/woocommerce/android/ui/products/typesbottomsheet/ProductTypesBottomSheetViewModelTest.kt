package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class ProductTypesBottomSheetViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductTypesBottomSheetViewModel
    private val bottomSheetBuilder: ProductTypeBottomSheetBuilder = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(
            SiteModel().apply {
                url = "https://test.com"
                setAdminUrl("https://test.com/wp-admin/")
            }
        )
    }

    @Before
    fun setUp() = testBlocking {
        whenever(bottomSheetBuilder.buildBottomSheetList()).thenReturn(uiItems)
    }

    @Test
    fun `given is Add Product flow, when loading product types, then product types not filtered`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
        )

        assertThat(viewModel.productTypesBottomSheetList.value).isEqualTo(uiItems)
    }

    @Test
    fun `given is not Add Product flow, when loading product types, then product types is filtered`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(
                isAddProduct = false,
                currentProductType = "simple",
                isCurrentProductVirtual = false
            ).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
        )

        assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 1)
    }

    @Test
    fun `given current type is virtual, when loading product types, then only virtual type is filtered out`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(
                isAddProduct = false,
                currentProductType = "simple",
                isCurrentProductVirtual = true
            ).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
        )

        assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 1)
        assertThat(viewModel.productTypesBottomSheetList.value!![0].isVirtual).isFalse
    }

    @Test
    fun `when bookable service type selected for add product, then webview event is triggered`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
        )
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever { events.add(it) }

        val bookableServiceItem = ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem(
            type = ProductType.BOOKABLE_SERVICE,
            titleResource = 0,
            descResource = 0,
            iconResource = 0
        )
        viewModel.onProductTypeSelected(bookableServiceItem)

        assertThat(events).hasAtLeastOneElementOfType(
            MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView::class.java
        )
        val webViewEvent = events.filterIsInstance<MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView>().first()
        assertThat(webViewEvent.url).contains(
            ProductTypesBottomSheetViewModel.BOOKABLE_SERVICE_CREATION_PATH
        )
        assertThat(events).hasAtLeastOneElementOfType(MultiLiveEvent.Event.Exit::class.java)
    }

    private val uiItems: List<ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem> = listOf(
        ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem(
            type = ProductType.SIMPLE,
            titleResource = 0,
            descResource = 0,
            iconResource = 0
        ),
        ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem(
            type = ProductType.SIMPLE,
            titleResource = 0,
            descResource = 0,
            iconResource = 0,
            isVirtual = true
        ),
        ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem(
            type = ProductType.GROUPED,
            titleResource = 0,
            descResource = 0,
            iconResource = 0
        )
    )
}
