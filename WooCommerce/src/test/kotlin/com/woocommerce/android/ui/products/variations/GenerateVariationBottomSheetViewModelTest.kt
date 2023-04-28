package com.woocommerce.android.ui.products.variations

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class GenerateVariationBottomSheetViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: GenerateVariationBottomSheetViewModel
    private val savedStateHandle: SavedStateHandle = mock()
    private val eventObserver: Observer<Event> = mock()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        viewModel = GenerateVariationBottomSheetViewModel(savedStateHandle)
        viewModel.event.observeForever(eventObserver)
    }

    @Test
    fun `test onGenerateAllVariationsClicked triggers GenerateAllVariations event`() {
        viewModel.onGenerateAllVariationsClicked()

        verify(eventObserver).onChanged(GenerateVariationBottomSheetViewModel.GenerateAllVariations)
    }

    @Test
    fun `test onAddNewVariationClicked triggers AddNewVariation event`() {
        viewModel.onAddNewVariationClicked()

        verify(eventObserver).onChanged(GenerateVariationBottomSheetViewModel.AddNewVariation)
    }
}
