package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ProductShippingViewModelTest : BaseUnitTest() {
    private val parameterRepository: ParameterRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()

    private val initialData = ShippingData(
        10f, 9f, 8f, 7f, "Class 1", 1
    )

    private val expectedData = ShippingData(
        1f, 2f, 3f, 4f, "Class 2", 2
    )

    private val coroutineDispatchers = CoroutineDispatchers(
        Dispatchers.Unconfined,
        Dispatchers.Unconfined,
        Dispatchers.Unconfined
    )
    private lateinit var viewModel: ProductShippingViewModel

    @Before
    fun setup() {
        viewModel = createViewModel(RequestCodes.PRODUCT_DETAIL_SHIPPING)
    }

    private fun createViewModel(requestCode: Int): ProductShippingViewModel {
        val savedState = SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ProductShippingFragmentArgs(requestCode, initialData)
        )
        return spy(
            ProductShippingViewModel(
                savedState,
                coroutineDispatchers,
                parameterRepository,
                productDetailRepository
            )
        )
    }

    @Test
    fun `Test that the initial data is displayed correctly`() = test {
        var actual: ShippingData? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.shippingData
        }

        assertThat(actual).isEqualTo(initialData)
    }

    @Test
    fun `Updates the data`() = test {
        var actual: ShippingData? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.shippingData
        }

        viewModel.onDataChanged(
            expectedData.weight,
            expectedData.length,
            expectedData.width,
            expectedData.height,
            expectedData.shippingClassSlug,
            expectedData.shippingClassId
        )

        assertThat(actual).isEqualTo(expectedData)
    }


    @Test
    fun `Test that a discard dialog isn't shown if no data changed`() = test {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        assertThat(events).isEmpty()

        viewModel.onExit()

        assertThat(events.singleOrNull { it is Exit }).isNotNull
        assertThat(events.any { it is ShowDiscardDialog }).isFalse()
        assertThat(events.any { it is ExitWithResult<*> }).isFalse()
    }

    @Test
    fun `Test that a discard dialog is shown if data changed`() = test {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onDataChanged(expectedData.weight)

        viewModel.onExit()

        assertThat(events.singleOrNull { it is ShowDiscardDialog }).isNotNull
        assertThat(events.any { it is ExitWithResult<*> }).isFalse()
        assertThat(events.any { it is Exit }).isFalse()
    }


    @Test
    fun `Test that a the correct data is returned when Done button clicked`() = test {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onDataChanged(
            expectedData.weight,
            expectedData.length,
            expectedData.width,
            expectedData.height,
            expectedData.shippingClassSlug,
            expectedData.shippingClassId
        )

        viewModel.onDoneButtonClicked()

        assertThat(events.any { it is ShowDiscardDialog }).isFalse()
        assertThat(events.any { it is Exit }).isFalse()

        @Suppress("UNCHECKED_CAST")
        val result = events.single { it is ExitWithResult<*> } as ExitWithResult<ShippingData>

        assertThat(result.data).isEqualTo(expectedData)
    }

    @Test
    fun `Test that the done button is only shown if data changed`() = test {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isDoneButtonVisible).isFalse()

        viewModel.onDataChanged(expectedData.weight)

        assertThat(viewState?.isDoneButtonVisible).isTrue()

        viewModel.onDataChanged(initialData.weight)

        assertThat(viewState?.isDoneButtonVisible).isFalse()
    }


    @Test
    fun `Test that the class section is visible for products`() = test {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isShippingClassSectionVisible).isTrue()
    }

    @Test
    fun `Test that the class section is not visible for variations`() = test {
        viewModel = createViewModel(RequestCodes.VARIATION_DETAIL_SHIPPING)

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isShippingClassSectionVisible).isFalse()
    }
}
