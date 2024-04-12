package com.woocommerce.android.ui.products.shipping

import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ProductShippingViewModelTest : BaseUnitTest() {
    private val parameterRepository: ParameterRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val initialData = ProductShippingViewModel.ShippingData(
        10f,
        9f,
        8f,
        7f,
        "Class 1",
        1
    )

    private val expectedData = ProductShippingViewModel.ShippingData(
        1f,
        2f,
        3f,
        4f,
        "Class 2",
        2
    )

    private lateinit var viewModel: ProductShippingViewModel

    @Before
    fun setup() {
        viewModel = createViewModel(RequestCodes.PRODUCT_DETAIL_SHIPPING)
    }

    private fun createViewModel(requestCode: Int): ProductShippingViewModel {
        val savedState = ProductShippingFragmentArgs(requestCode, initialData).toSavedStateHandle()
        return spy(
            ProductShippingViewModel(
                savedState,
                parameterRepository,
                productDetailRepository,
                analyticsTracker,
            )
        )
    }

    @Test
    fun `Test that the initial data is displayed correctly`() = testBlocking {
        var actual: ProductShippingViewModel.ShippingData? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.shippingData
        }

        Assertions.assertThat(actual).isEqualTo(initialData)
    }

    @Test
    fun `Test that when data is changed the view state is updated`() =
        testBlocking {
            var actual: ProductShippingViewModel.ShippingData? = null
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

            Assertions.assertThat(actual).isEqualTo(expectedData)
        }

    @Test
    fun `Test that a discard dialog isn't shown if no data changed`() =
        testBlocking {
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            Assertions.assertThat(events).isEmpty()

            viewModel.onExit()

            Assertions.assertThat(events.singleOrNull { it is MultiLiveEvent.Event.Exit }).isNotNull
            Assertions.assertThat(events.any { it is MultiLiveEvent.Event.ShowDialog }).isFalse()
            Assertions.assertThat(events.any { it is MultiLiveEvent.Event.ExitWithResult<*> }).isFalse()
        }

    @Test
    fun `Test that a the correct data is returned when exiting`() = testBlocking {
        val events = mutableListOf<MultiLiveEvent.Event>()
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

        Assertions.assertThat(events.any { it is MultiLiveEvent.Event.ShowDialog }).isFalse()
        Assertions.assertThat(events.any { it is MultiLiveEvent.Event.Exit }).isFalse()

        @Suppress("UNCHECKED_CAST")
        val result = events.single {
            it is MultiLiveEvent.Event.ExitWithResult<*>
        } as MultiLiveEvent.Event.ExitWithResult<ProductShippingViewModel.ShippingData>

        Assertions.assertThat(result.data).isEqualTo(expectedData)
    }

    @Test
    fun `Test that the class section is visible for products`() = testBlocking {
        var viewState: ProductShippingViewModel.ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        Assertions.assertThat(viewState?.isShippingClassSectionVisible).isTrue()
    }

    @Test
    fun `Test that the class section is not visible for variations`() =
        testBlocking {
            viewModel = createViewModel(RequestCodes.VARIATION_DETAIL_SHIPPING)

            var viewState: ProductShippingViewModel.ViewState? = null
            viewModel.viewStateData.observeForever { _, new ->
                viewState = new
            }

            Assertions.assertThat(viewState?.isShippingClassSectionVisible).isFalse()
        }

    @Test
    fun `Send tracks event upon exit if there was no changes`() {
        // when
        viewModel.onExit()

        // then
        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to false)
        )
    }

    @Test
    fun `Send tracks event upon exit if there was a change`() {
        // when
        viewModel.onDataChanged(weight = 31415f)
        viewModel.onExit()

        // then
        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to true)
        )
    }
}
