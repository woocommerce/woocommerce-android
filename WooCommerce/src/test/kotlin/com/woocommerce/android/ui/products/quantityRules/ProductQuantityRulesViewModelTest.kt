package com.woocommerce.android.ui.products.quantityRules

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.models.QuantityRules
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
class ProductQuantityRulesViewModelTest : BaseUnitTest() {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val initialData = QuantityRules(4, 8, 2)
    private val expectedData = QuantityRules(8, 16, 4)
    private val exitAnalyticsEvent = AnalyticsEvent.PRODUCT_DETAIL_QUANTITY_RULES_DONE_BUTTON_TAPPED
    private lateinit var viewModel: ProductQuantityRulesViewModel

    @Before
    fun setup() {
        viewModel = createViewModel(initialData, exitAnalyticsEvent)
    }

    private fun createViewModel(
        quantityRules: QuantityRules,
        exitAnalyticsEvent: AnalyticsEvent
    ): ProductQuantityRulesViewModel {
        val savedState = ProductQuantityRulesFragmentArgs(quantityRules, exitAnalyticsEvent).toSavedStateHandle()
        return spy(
            ProductQuantityRulesViewModel(
                savedState,
                analyticsTracker
            )
        )
    }

    @Test
    fun `Test that the initial data is displayed correctly`() = testBlocking {
        var actual: QuantityRules? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.quantityRules
        }

        Assertions.assertThat(actual).isEqualTo(initialData)
    }

    @Test
    fun `Test that when data is changed the view state is updated`() =
        testBlocking {
            var actual: QuantityRules? = null
            viewModel.viewStateData.observeForever { _, new ->
                actual = new.quantityRules
            }

            viewModel.onDataChanged(
                expectedData.min,
                expectedData.max,
                expectedData.groupOf
            )

            Assertions.assertThat(actual).isEqualTo(expectedData)
        }

    @Test
    fun `Test that a the correct data is returned when exiting`() = testBlocking {
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onDataChanged(
            expectedData.min,
            expectedData.max,
            expectedData.groupOf
        )

        viewModel.onExit()

        Assertions.assertThat(events.any { it is MultiLiveEvent.Event.ShowDialog }).isFalse()
        Assertions.assertThat(events.any { it is MultiLiveEvent.Event.Exit }).isFalse()

        @Suppress("UNCHECKED_CAST")
        val result = events.single {
            it is MultiLiveEvent.Event.ExitWithResult<*>
        } as MultiLiveEvent.Event.ExitWithResult<QuantityRules>

        Assertions.assertThat(result.data).isEqualTo(expectedData)
    }

    @Test
    fun `Send tracks event upon exit if there was no changes`() {
        // when
        viewModel.onExit()

        // then
        verify(analyticsTracker).track(
            exitAnalyticsEvent,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to false)
        )
    }

    @Test
    fun `Send tracks event upon exit if there was a change`() {
        // when
        viewModel.onDataChanged(min = initialData.min?.plus(2))
        viewModel.onExit()

        // then
        verify(analyticsTracker).track(
            exitAnalyticsEvent,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to true)
        )
    }
}
