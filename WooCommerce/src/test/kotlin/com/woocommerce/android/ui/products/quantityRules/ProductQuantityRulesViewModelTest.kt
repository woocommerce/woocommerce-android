package com.woocommerce.android.ui.products.quantityRules

import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.spy

@ExperimentalCoroutinesApi
class ProductQuantityRulesViewModelTest : BaseUnitTest() {
    private val initialData = QuantityRules(4, 8, 2)
    private val expectedData = QuantityRules(8, 16, 4)
    private lateinit var viewModel: ProductQuantityRulesViewModel

    @Before
    fun setup() {
        viewModel = createViewModel(initialData)
    }

    private fun createViewModel(quantityRules: QuantityRules): ProductQuantityRulesViewModel {
        val savedState = ProductQuantityRulesFragmentArgs(quantityRules).toSavedStateHandle()
        return spy(
            ProductQuantityRulesViewModel(
                savedState
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
}
