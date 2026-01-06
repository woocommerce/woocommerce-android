package com.woocommerce.android.ui.pospromo

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PosPromoViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: PosPromoViewModel

    private fun createViewModel() {
        viewModel = PosPromoViewModel(savedStateHandle = mock())
    }

    @Test
    fun `given viewModel created, when state observed, then currentPage is 0`() = testBlocking {
        createViewModel()

        assertThat(viewModel.state.value.currentPage).isEqualTo(0)
    }

    @Test
    fun `given viewModel on first page, when onNextClick called, then currentPage increments`() = testBlocking {
        createViewModel()

        viewModel.onNextClick()

        assertThat(viewModel.state.value.currentPage).isEqualTo(1)
    }

    @Test
    fun `given viewModel on last page, when onNextClick called, then currentPage stays at max`() = testBlocking {
        createViewModel()
        val maxPage = viewModel.state.value.pages.size - 1

        repeat(maxPage + 5) {
            viewModel.onNextClick()
        }

        assertThat(viewModel.state.value.currentPage).isEqualTo(maxPage)
    }
}
