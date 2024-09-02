package com.woocommerce.android.ui.products.filter

import com.woocommerce.android.ui.products.filter.ProductFilterListViewModel.FilterListItemUiModel
import com.woocommerce.android.ui.products.filter.ProductFilterListViewModel.FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption

@OptIn(ExperimentalCoroutinesApi::class)
class FilterListItemUiModelTest : BaseUnitTest() {

    @Test
    fun `firstSelectedOption returns null when no options are selected`() {
        val filterListItemUiModel = FilterListItemUiModel(
            filterItemKey = ProductFilterOption.STATUS,
            filterItemName = "Status",
            filterOptionListItems = listOf(
                DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = "Option 1",
                    filterOptionItemValue = "option_1",
                    isSelected = false
                ),
                DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = "Option 2",
                    filterOptionItemValue = "option_2",
                    isSelected = false
                )
            )
        )

        assertThat(filterListItemUiModel.firstSelectedOption).isNull()
    }

    @Test
    fun `firstSelectedOption returns the name of the first selected option`() {
        val filterListItemUiModel = FilterListItemUiModel(
            filterItemKey = ProductFilterOption.STATUS,
            filterItemName = "Status",
            filterOptionListItems = listOf(
                DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = "Option 1",
                    filterOptionItemValue = "option_1",
                    isSelected = true
                ),
                DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = "Option 2",
                    filterOptionItemValue = "option_2",
                    isSelected = false
                )
            )
        )

        assertThat(filterListItemUiModel.firstSelectedOption).isEqualTo("Option 1")
    }

    @Test
    fun `firstSelectedOption returns the name of the first selected option when multiple options are selected`() {
        val filterListItemUiModel = FilterListItemUiModel(
            filterItemKey = ProductFilterOption.STATUS,
            filterItemName = "Status",
            filterOptionListItems = listOf(
                DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = "Option 1",
                    filterOptionItemValue = "option_1",
                    isSelected = true
                ),
                DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = "Option 2",
                    filterOptionItemValue = "option_2",
                    isSelected = true
                )
            )
        )

        assertThat(filterListItemUiModel.firstSelectedOption).isEqualTo("Option 1")
    }

    @Test
    fun `firstSelectedOption returns null when filterOptionListItems is empty`() {
        val filterListItemUiModel = FilterListItemUiModel(
            filterItemKey = ProductFilterOption.STATUS,
            filterItemName = "Status",
            filterOptionListItems = emptyList()
        )

        assertThat(filterListItemUiModel.firstSelectedOption).isNull()
    }
}
