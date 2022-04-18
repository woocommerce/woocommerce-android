package com.woocommerce.android.ui.products

import com.woocommerce.android.RequestCodes
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@ExperimentalCoroutinesApi
class ProductShippingViewModelTest : BaseUnitTest() {
    private val parameterRepository: ParameterRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()

    private val initialData = ShippingData(
        10f, 9f, 8f, 7f, "Class 1", 1
    )

    private val expectedData = ShippingData(
        1f, 2f, 3f, 4f, "Class 2", 2
    )

    private lateinit var viewModel: ProductShippingViewModel

    @Before
    fun setup() {
        viewModel = createViewModel(RequestCodes.PRODUCT_DETAIL_SHIPPING)
    }

    private fun createViewModel(requestCode: Int): ProductShippingViewModel {
        val savedState = ProductShippingFragmentArgs(requestCode, initialData).initSavedStateHandle()
        return spy(
            ProductShippingViewModel(
                savedState,
                parameterRepository,
                productDetailRepository
            )
        )
    }

    @Test
    fun `Test that the initial data is displayed correctly`() = testBlocking {
        var actual: ShippingData? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.shippingData
        }

        assertThat(actual).isEqualTo(initialData)
    }

    @Test
    fun `Test that when data is changed the view state is updated`() =
        testBlocking {
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
    fun `Test that a discard dialog isn't shown if no data changed`() =
        testBlocking {
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            assertThat(events).isEmpty()

            viewModel.onExit()

            assertThat(events.singleOrNull { it is Exit }).isNotNull
            assertThat(events.any { it is ShowDialog }).isFalse()
            assertThat(events.any { it is ExitWithResult<*> }).isFalse()
        }

    @Test
    fun `Test that a the correct data is returned when exiting`() = testBlocking {
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

        viewModel.onExit()

        assertThat(events.any { it is ShowDialog }).isFalse()
        assertThat(events.any { it is Exit }).isFalse()

        @Suppress("UNCHECKED_CAST")
        val result = events.single { it is ExitWithResult<*> } as ExitWithResult<ShippingData>

        assertThat(result.data).isEqualTo(expectedData)
    }

    @Test
    fun `Test that the class section is visible for products`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isShippingClassSectionVisible).isTrue()
    }

    @Test
    fun `Test that the class section is not visible for variations`() =
        testBlocking {
            viewModel = createViewModel(RequestCodes.VARIATION_DETAIL_SHIPPING)

            var viewState: ViewState? = null
            viewModel.viewStateData.observeForever { _, new ->
                viewState = new
            }

            assertThat(viewState?.isShippingClassSectionVisible).isFalse()
        }
}
