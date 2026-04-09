package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.details.webview.ProductDetailWebViewViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
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
    private val productDetailRepository: ProductDetailRepository = mock()

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
            productDetailRepository,
        )

        assertThat(viewModel.productTypesBottomSheetList.value).isEqualTo(uiItems)
    }

    @Test
    fun `given is not Add Product flow, when loading product types, then current type and webview-only types are filtered`() =
        testBlocking {
            viewModel = ProductTypesBottomSheetViewModel(
                ProductTypesBottomSheetFragmentArgs(
                    isAddProduct = false,
                    currentProductType = "simple",
                    isCurrentProductVirtual = false
                ).toSavedStateHandle(),
                bottomSheetBuilder,
                selectedSite,
                productDetailRepository,
            )

            // Current type (SIMPLE non-virtual) and webview-only type (BOOKABLE_SERVICE) should be filtered out
            assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 2)
            assertThat(viewModel.productTypesBottomSheetList.value!!.none { !it.supportsNativeEditor }).isTrue()
        }

    @Test
    fun `given current type is virtual, when loading product types, then only virtual type and webview-only types are filtered out`() =
        testBlocking {
            viewModel = ProductTypesBottomSheetViewModel(
                ProductTypesBottomSheetFragmentArgs(
                    isAddProduct = false,
                    currentProductType = "simple",
                    isCurrentProductVirtual = true
                ).toSavedStateHandle(),
                bottomSheetBuilder,
                selectedSite,
                productDetailRepository,
            )

            // Virtual SIMPLE and webview-only BOOKABLE_SERVICE should be filtered out
            assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 2)
            assertThat(viewModel.productTypesBottomSheetList.value!![0].isVirtual).isFalse
            assertThat(viewModel.productTypesBottomSheetList.value!!.none { !it.supportsNativeEditor }).isTrue()
        }

    @Test
    fun `given is Add Product flow, when loading product types, then webview-only types are included`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
            productDetailRepository,
        )

        assertThat(viewModel.productTypesBottomSheetList.value!!.any { !it.supportsNativeEditor }).isTrue()
    }

    @Test
    fun `when bookable service type selected, then auto-draft is created and webview opens with edit URL`() = testBlocking {
        val fakeProductId = 123L
        whenever(productDetailRepository.createAutoDraftProduct(any()))
            .thenReturn(Result.success(fakeProductId))

        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
            productDetailRepository,
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
            ProductDetailWebViewViewModel.BOOKABLE_SERVICE_PATH
        )
        assertThat(webViewEvent.url).contains("$fakeProductId")
        assertThat(events).hasAtLeastOneElementOfType(MultiLiveEvent.Event.Exit::class.java)
        assertThat(viewModel.isCreatingProduct.value).isFalse()
    }

    @Test
    fun `when bookable service creation fails, then snackbar is shown`() = testBlocking {
        whenever(productDetailRepository.createAutoDraftProduct(any()))
            .thenReturn(Result.failure(Exception("Failed")))

        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
            selectedSite,
            productDetailRepository,
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
            MultiLiveEvent.Event.ShowSnackbar::class.java
        )
        assertThat(viewModel.isCreatingProduct.value).isFalse()
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
        ),
        ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem(
            type = ProductType.BOOKABLE_SERVICE,
            titleResource = 0,
            descResource = 0,
            iconResource = 0,
            supportsNativeEditor = false
        )
    )
}
