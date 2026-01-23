package com.woocommerce.android.ui.pospromo

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PosPromoViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: PosPromoViewModel
    private val analyticsTracker: PosPromoAnalyticsTracker = mock()
    private val utmProvider: PosPromoUtmProvider = mock()

    private fun createViewModel() {
        whenever(utmProvider.getUrlWithUtmParams(any())).thenReturn("https://test.url")
        viewModel = PosPromoViewModel(
            savedStateHandle = mock(),
            analyticsTracker = analyticsTracker,
            utmProvider = utmProvider
        )
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

    @Test
    fun `when viewModel created, then modal viewed is tracked`() = testBlocking {
        createViewModel()

        verify(analyticsTracker).trackModalViewed()
    }

    @Test
    fun `when viewModel created, then first slide viewed is tracked`() = testBlocking {
        createViewModel()

        verify(analyticsTracker).trackSlideViewed(slideIndex = 0)
    }

    @Test
    fun `when onNextClick called, then next slide viewed is tracked`() = testBlocking {
        createViewModel()

        viewModel.onNextClick()

        verify(analyticsTracker).trackSlideViewed(slideIndex = 1)
    }

    @Test
    fun `when onDismiss called, then modal dismissed is tracked`() = testBlocking {
        createViewModel()

        viewModel.onDismiss()

        verify(analyticsTracker).trackModalDismissed()
    }

    @Test
    fun `when onExploreClick called, then explore clicked is tracked`() = testBlocking {
        createViewModel()

        viewModel.onExploreClick()

        verify(analyticsTracker).trackExploreClicked()
    }

    @Test
    fun `when onExploreClick called, then NavigateToExplore event is triggered with UTM url`() = testBlocking {
        val expectedUrl = "https://test.url"
        createViewModel()

        viewModel.onExploreClick()

        assertThat(viewModel.event.value).isEqualTo(PosPromoViewModel.NavigateToExplore(expectedUrl))
    }
}
