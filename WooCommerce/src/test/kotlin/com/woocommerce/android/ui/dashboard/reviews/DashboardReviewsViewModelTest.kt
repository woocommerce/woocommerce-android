package com.woocommerce.android.ui.dashboard.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.ui.reviews.ProductReviewTestUtils
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.ui.reviews.ReviewModerationHandler
import com.woocommerce.android.ui.reviews.ReviewModerationRequest
import com.woocommerce.android.ui.reviews.ReviewModerationStatus
import com.woocommerce.android.util.advanceTimeAndRun
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardReviewsViewModelTest : BaseUnitTest() {
    private val sampleReviews = (0L..3L).map {
        ProductReviewTestUtils.generateProductReview(
            id = it,
            productId = 1L,
            isRead = it != 0L,
        ).copy(
            status = if (it == 0L) ProductReviewStatus.HOLD.toString() else ProductReviewStatus.APPROVED.toString()
        )
    }

    private val reviewListRepository: ReviewListRepository = mock {
        onBlocking { getCachedProductReviews(anyOrNull()) } doReturn sampleReviews
        onBlocking { fetchMostRecentReviews(any()) } doReturn Result.success(Unit)
    }
    private val reviewModerationHandler: ReviewModerationHandler = mock {
        on { pendingModerationStatus } doReturn flowOf(emptyList())
    }
    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn emptyFlow()
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: DashboardReviewsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = DashboardReviewsViewModel(
            savedStateHandle = SavedStateHandle(),
            parentViewModel = parentViewModel,
            reviewListRepository = reviewListRepository,
            reviewModerationHandler = reviewModerationHandler,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `when loading the card, then show the loading state`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.captureValues().first()

        assertThat(viewState).isInstanceOf(DashboardReviewsViewModel.ViewState.Loading::class.java)
    }

    @Test
    fun `given successful fetch of reviews, when loading the card, then show the reviews`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState).isInstanceOf(DashboardReviewsViewModel.ViewState.Success::class.java)
        assertThat((viewState as DashboardReviewsViewModel.ViewState.Success).reviews)
            .isEqualTo(sampleReviews.take(DashboardReviewsViewModel.MAX_REVIEWS))
    }

    @Test
    fun `given failure while fetching reviews, when loading the card, then show the error state`() = testBlocking {
        setup {
            whenever(reviewListRepository.fetchMostRecentReviews(any())).doReturn(Result.failure(Exception()))
        }

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState).isEqualTo(DashboardReviewsViewModel.ViewState.Error)
    }

    @Test
    fun `given failure while fetching reviews, when retrying, then reload reviews`() = testBlocking {
        setup {
            whenever(reviewListRepository.fetchMostRecentReviews(any()))
                .thenReturn(Result.failure(Exception()))
                .thenReturn(Result.success(Unit))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onRetryClicked()
        }.last()

        assertThat(viewState).isInstanceOf(DashboardReviewsViewModel.ViewState.Success::class.java)
    }

    @Test
    fun `when status changes, then load filtered reviews`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onFilterSelected(ProductReviewStatus.HOLD)
        }.last()

        assertThat(viewState).isInstanceOf(DashboardReviewsViewModel.ViewState.Success::class.java)
        assertThat((viewState as DashboardReviewsViewModel.ViewState.Success).reviews)
            .isEqualTo(
                sampleReviews.filter { it.status == ProductReviewStatus.HOLD.toString() }
                    .take(DashboardReviewsViewModel.MAX_REVIEWS)
            )
    }

    @Test
    fun `when force refreshing, then fetch before showing reviews`() = testBlocking {
        val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
        setup {
            whenever(reviewListRepository.fetchMostRecentReviews(any())).doSuspendableAnswer {
                delay(500L)
                Result.success(Unit)
            }
            whenever(parentViewModel.refreshTrigger).doReturn(refreshTrigger)
        }

        val viewStates = viewModel.viewState.captureValues()

        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
        val loadingState = viewStates.last()
        advanceTimeAndRun(500L)
        val successState = viewStates.last()

        assertThat(loadingState).isInstanceOf(DashboardReviewsViewModel.ViewState.Loading::class.java)
        assertThat(successState).isInstanceOf(DashboardReviewsViewModel.ViewState.Success::class.java)
    }

    @Test
    fun `when there is a pending delete operation, then filter out the deleted review`() = testBlocking {
        setup {
            whenever(reviewModerationHandler.pendingModerationStatus)
                .doReturn(
                    flowOf(
                        listOf(
                            ReviewModerationStatus(
                                request = ReviewModerationRequest(
                                    review = sampleReviews[0],
                                    newStatus = ProductReviewStatus.TRASH
                                ),
                                actionStatus = ActionStatus.PENDING
                            )
                        )
                    )
                )
        }

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState).isInstanceOf(DashboardReviewsViewModel.ViewState.Success::class.java)
        assertThat((viewState as DashboardReviewsViewModel.ViewState.Success).reviews)
            .doesNotContain(sampleReviews[0])
    }

    @Test
    fun `when there is a pending status change, then update the shown review`() = testBlocking {
        setup {
            whenever(reviewModerationHandler.pendingModerationStatus)
                .doReturn(
                    flowOf(
                        listOf(
                            ReviewModerationStatus(
                                request = ReviewModerationRequest(
                                    review = sampleReviews[0],
                                    newStatus = ProductReviewStatus.APPROVED
                                ),
                                actionStatus = ActionStatus.PENDING
                            )
                        )
                    )
                )
        }

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState).isInstanceOf(DashboardReviewsViewModel.ViewState.Success::class.java)
        assertThat((viewState as DashboardReviewsViewModel.ViewState.Success).reviews[0].status)
            .isEqualTo(ProductReviewStatus.APPROVED.toString())
    }

    @Test
    fun `when tapping on a review, then open review details`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onReviewClicked(sampleReviews[0])
        }.last()

        assertThat(event).isEqualTo(DashboardReviewsViewModel.OpenReviewDetail(sampleReviews[0]))
    }

    @Test
    fun `when tapping on the view all button, then open the reviews list`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onViewAllClicked()
        }.last()

        assertThat(event).isEqualTo(DashboardReviewsViewModel.OpenReviewsList)
    }
}
