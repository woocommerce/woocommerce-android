package com.woocommerce.android.ui.orders.wooshippinglabels.hazmat

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelHazmatFormViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WooShippingLabelHazmatFormViewModel

    @Before
    fun setup() {
        viewModel = WooShippingLabelHazmatFormViewModel(SavedStateHandle())
    }

    @Test
    fun `when initialized, then viewState has default values`() = testBlocking {
        // Given/When
        var capturedViewState: WooShippingLabelHazmatFormViewModel.ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // Then
        assertThat(capturedViewState?.containsHazmatChecked).isFalse()
        assertThat(capturedViewState?.currentHazmatSelection).isNull()
    }

    @Test
    fun `when onContainsHazmatChanged is called with true, then viewState is updated`() = testBlocking {
        // Given
        var capturedViewState: WooShippingLabelHazmatFormViewModel.ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onContainsHazmatChanged(true)

        // Then
        assertThat(capturedViewState?.containsHazmatChecked).isTrue()
    }

    @Test
    fun `when onContainsHazmatChanged is called with false, then viewState is updated`() = testBlocking {
        // Given
        var capturedViewState: WooShippingLabelHazmatFormViewModel.ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onContainsHazmatChanged(false)

        // Then
        assertThat(capturedViewState?.containsHazmatChecked).isFalse()
    }

    @Test
    fun `when onSelectCategoryClick is called, then OnSelectCategoryClicked event is triggered`() = testBlocking {
        // Given
        var capturedEvent: WooShippingLabelHazmatFormViewModel.OnSelectCategoryClicked? = null
        viewModel.event.observeForever {
            capturedEvent = it as? WooShippingLabelHazmatFormViewModel.OnSelectCategoryClicked
        }

        // When
        viewModel.onSelectCategoryClick()

        // Then
        assertThat(capturedEvent).isInstanceOf(WooShippingLabelHazmatFormViewModel.OnSelectCategoryClicked::class.java)
    }

    @Test
    fun `when onUrlSelected is called, then OnUrlSelected event is triggered with correct URL`() = testBlocking {
        // Given
        val testUrl = "https://test.com"
        var capturedEvent: WooShippingLabelHazmatFormViewModel.OnUrlSelected? = null
        viewModel.event.observeForever {
            capturedEvent = it as? WooShippingLabelHazmatFormViewModel.OnUrlSelected
        }

        // When
        viewModel.onUrlSelected(testUrl)

        // Then
        assertThat(capturedEvent).isNotNull()
        assertThat(capturedEvent?.url).isEqualTo(testUrl)
    }

    @Test
    fun `when onHazmatCategorySelected is called, then viewState is updated with selected category`() = testBlocking {
        // Given
        val selectedCategory = ShippingLabelHazmatCategory.CLASS_1
        var capturedViewState: WooShippingLabelHazmatFormViewModel.ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onContainsHazmatChanged(true)
        viewModel.onHazmatCategorySelected(selectedCategory)

        // Then
        assertThat(capturedViewState?.currentHazmatSelection).isEqualTo(selectedCategory)
    }

    @Test
    fun `when multiple state changes occur, then viewState reflects latest values`() = testBlocking {
        // Given
        var capturedViewState: WooShippingLabelHazmatFormViewModel.ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onContainsHazmatChanged(true)
        viewModel.onHazmatCategorySelected(ShippingLabelHazmatCategory.CLASS_1)
        viewModel.onHazmatCategorySelected(ShippingLabelHazmatCategory.CLASS_3)

        // Then
        assertThat(capturedViewState?.containsHazmatChecked).isTrue()
        assertThat(capturedViewState?.currentHazmatSelection).isEqualTo(ShippingLabelHazmatCategory.CLASS_3)
    }

    @Test
    fun `when onContainsHazmatChanged is set as false, then currentHazmatSelection is null`() = testBlocking {
        // Given
        var capturedViewState: WooShippingLabelHazmatFormViewModel.ViewState? = null
        viewModel.viewState.observeForever {
            capturedViewState = it
        }

        // When
        viewModel.onContainsHazmatChanged(true)
        viewModel.onHazmatCategorySelected(ShippingLabelHazmatCategory.CLASS_1)
        viewModel.onContainsHazmatChanged(false)

        // Then
        assertThat(capturedViewState?.containsHazmatChecked).isFalse()
        assertThat(capturedViewState?.currentHazmatSelection).isNull()
    }
}
