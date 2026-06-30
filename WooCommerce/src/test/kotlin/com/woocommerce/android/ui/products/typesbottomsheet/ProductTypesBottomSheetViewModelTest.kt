package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ProductTypesBottomSheetViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductTypesBottomSheetViewModel
    private val bottomSheetBuilder: ProductTypeBottomSheetBuilder = mock()

    @Before
    fun setUp() = testBlocking {
        whenever(bottomSheetBuilder.buildBottomSheetList()).thenReturn(uiItems)
    }

    @Test
    fun `given is Add Product flow, when loading product types, then product types not filtered`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
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
            )

            // Current type (SIMPLE non-virtual) and webview-only type should be filtered out
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
            )

            // Virtual SIMPLE and webview-only type should be filtered out
            assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 2)
            assertThat(viewModel.productTypesBottomSheetList.value!![0].isVirtual).isFalse
            assertThat(viewModel.productTypesBottomSheetList.value!!.none { !it.supportsNativeEditor }).isTrue()
        }

    @Test
    fun `given is Add Product flow, when loading product types, then webview-only types are included`() = testBlocking {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).toSavedStateHandle(),
            bottomSheetBuilder,
        )

        assertThat(viewModel.productTypesBottomSheetList.value!!.any { !it.supportsNativeEditor }).isTrue()
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
            type = ProductType.EXTERNAL,
            titleResource = 0,
            descResource = 0,
            iconResource = 0,
            supportsNativeEditor = false
        )
    )
}
