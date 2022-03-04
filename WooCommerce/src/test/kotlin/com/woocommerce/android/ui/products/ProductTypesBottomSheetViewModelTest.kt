package com.woocommerce.android.ui.products

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ProductTypesBottomSheetViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductTypesBottomSheetViewModel
    private val appPrefs: AppPrefs = mock()
    private val bottomSheetBuilder: ProductTypeBottomSheetBuilder = mock()

    @Test
    fun `given is Add Product flow, when loading product types, then product types not filtered`() {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(isAddProduct = true).initSavedStateHandle(),
            appPrefs, bottomSheetBuilder
        )
        whenever(bottomSheetBuilder.buildBottomSheetList()).thenReturn(uiItems)

        viewModel.loadProductTypes()

        assertThat(viewModel.productTypesBottomSheetList.value).isEqualTo(uiItems)
    }

    @Test
    fun `given is not Add Product flow, when loading product types, then product types is filtered`() {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(
                isAddProduct = false,
                currentProductType = "simple",
                isCurrentProductVirtual = false
            ).initSavedStateHandle(),
            appPrefs, bottomSheetBuilder
        )
        whenever(bottomSheetBuilder.buildBottomSheetList()).thenReturn(uiItems)

        viewModel.loadProductTypes()

        assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 1)
    }

    @Test
    fun `given current type is virtual, when loading product types, then only virtual type is filtered out`() {
        viewModel = ProductTypesBottomSheetViewModel(
            ProductTypesBottomSheetFragmentArgs(
                isAddProduct = false,
                currentProductType = "simple",
                isCurrentProductVirtual = true
            ).initSavedStateHandle(),
            appPrefs, bottomSheetBuilder
        )
        whenever(bottomSheetBuilder.buildBottomSheetList()).thenReturn(uiItems)

        viewModel.loadProductTypes()

        assertThat(viewModel.productTypesBottomSheetList.value!!.size).isEqualTo(uiItems.size - 1)
        assertThat(viewModel.productTypesBottomSheetList.value!![0].isVirtual).isFalse
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
