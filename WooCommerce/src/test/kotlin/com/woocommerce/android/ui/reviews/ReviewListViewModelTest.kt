package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.ui.reviews.domain.MarkAllReviewsAsSeen
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ReviewListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val reviewListRepository: ReviewListRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val markAllReviewsAsSeen: MarkAllReviewsAsSeen = mock()
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler = mock()
    private val reviewModerationHandler: ReviewModerationHandler = mock {
        on { pendingModerationStatus } doReturn emptyFlow()
    }

    private val reviews = ProductReviewTestUtils.generateProductReviewList()
    private lateinit var viewModel: ReviewListViewModel

    @Before
    fun setup() {
        viewModel = spy(
            ReviewListViewModel(
                savedState,
                networkStatus,
                dispatcher,
                reviewListRepository,
                markAllReviewsAsSeen,
                unseenReviewsCountHandler,
                reviewModerationHandler
            )
        )

        doReturn(true).whenever(networkStatus).isConnected()
    }

    /**
     * Tests that all the necessary logic is properly executed to properly
     * load reviews into the Review List View. This includes:
     *
     * - show/hide skeleton
     * - fetch and load reviews
     * - check for unread reviews
     */
    @Test
    fun `Load product reviews list successfully`() = testBlocking {
        doReturn(reviews).whenever(reviewListRepository).getCachedProductReviews()
        doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(flowOf(ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.SUCCESS)))
            .whenever(reviewListRepository).fetchProductReviews(loadMore = false)

        val reviewList = ArrayList<ProductReview>()
        var hasUnread = false
        val skeletonShown = mutableListOf<Boolean>()
        viewModel.reviewList.observeForever {
            // We know this will be called twice because the request to fetch the reviews
            // from the API will also result in passing results from db to the UI.
            reviewList.clear()
            reviewList.addAll(it)
        }
        viewModel.viewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        viewModel.start()

        verify(reviewListRepository, times(1)).fetchProductReviews(loadMore = false)
        verify(reviewListRepository, times(2)).getCachedProductReviews()
        assertThat(reviewList).isEqualTo(reviews)
        assertTrue(hasUnread)
    }

    @Test
    fun `Handle loading product reviews list while offline correctly`() = testBlocking {
        doReturn(reviews).whenever(reviewListRepository).getCachedProductReviews()
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        val skeletonShown = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
        }

        viewModel.start()

        verify(reviewListRepository, times(0)).fetchProductReviews(any(), any())
        verify(reviewListRepository, times(1)).getCachedProductReviews()
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Load product reviews list failed`() = testBlocking {
        doReturn(reviews).whenever(reviewListRepository).getCachedProductReviews()
        doReturn(false).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(flowOf(ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.ERROR)))
            .whenever(reviewListRepository).fetchProductReviews(loadMore = false)

        val reviewList = ArrayList<ProductReview>()
        var hasUnread = false
        val skeletonShown = mutableListOf<Boolean>()
        viewModel.reviewList.observeForever {
            // We know this will be called twice because the request to fetch the reviews
            // from the API will also result in passing results from db to the UI.
            reviewList.clear()
            reviewList.addAll(it)
        }
        viewModel.viewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(reviewListRepository, times(1)).fetchProductReviews(loadMore = false)
        verify(reviewListRepository, times(1)).getCachedProductReviews()
        assertThat(reviewList).isEqualTo(reviews)
        assertFalse(hasUnread)
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.review_fetch_error))
    }

    @Test
    fun `Show and hide review list skeleton correctly`() = testBlocking {
        doReturn(emptyList<ProductReview>()).whenever(reviewListRepository).getCachedProductReviews()
        doReturn(flowOf(ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.SUCCESS)))
            .whenever(reviewListRepository).fetchProductReviews(loadMore = false)

        val skeletonShown = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
        }

        viewModel.start()

        assertThat(skeletonShown).containsExactly(true, false)
    }

    @Test
    fun `given fetch review success and notification error, when view model started, then cached data shown`() =
        testBlocking {
            // GIVEN
            val reviews2 = ProductReviewTestUtils.generateProductReviewList()
            doReturn(
                reviews,
                reviews2,
            ).whenever(reviewListRepository).getCachedProductReviews()
            doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
            doReturn(
                flowOf(
                    ReviewListRepository.FetchReviewsResult.NotificationsFetched(RequestResult.ERROR),
                    ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.SUCCESS),
                )
            ).whenever(reviewListRepository).fetchProductReviews(loadMore = false)

            val reviewList = ArrayList<ProductReview>()
            viewModel.reviewList.observeForever {
                reviewList.clear()
                reviewList.addAll(it)
            }

            // WHEN
            viewModel.start()

            // THEN
            assertThat(reviewList).isEqualTo(reviews2)
        }

    @Test
    fun `given fetch review success and notification success, when view model started, then last cached data shown`() =
        testBlocking {
            // GIVEN
            val reviews2 = ProductReviewTestUtils.generateProductReviewList()
            val reviews3 = ProductReviewTestUtils.generateProductReviewList()
            doReturn(
                reviews,
                reviews2,
                reviews3,
            ).whenever(reviewListRepository).getCachedProductReviews()
            doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
            doReturn(
                flowOf(
                    ReviewListRepository.FetchReviewsResult.NotificationsFetched(RequestResult.SUCCESS),
                    ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.SUCCESS),
                )
            ).whenever(reviewListRepository).fetchProductReviews(loadMore = false)

            val reviewList = ArrayList<ProductReview>()
            viewModel.reviewList.observeForever {
                reviewList.clear()
                reviewList.addAll(it)
            }

            // WHEN
            viewModel.start()

            // THEN
            assertThat(reviewList).isEqualTo(reviews3)
        }

    @Test
    fun `given fetch review error and notification success, when view model started, then cached data shown`() =
        testBlocking {
            // GIVEN
            val reviews2 = ProductReviewTestUtils.generateProductReviewList()
            doReturn(
                reviews,
                reviews2,
            ).whenever(reviewListRepository).getCachedProductReviews()
            doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
            doReturn(
                flowOf(
                    ReviewListRepository.FetchReviewsResult.NotificationsFetched(RequestResult.SUCCESS),
                    ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.ERROR),
                )
            ).whenever(reviewListRepository).fetchProductReviews(loadMore = false)

            val reviewList = ArrayList<ProductReview>()
            viewModel.reviewList.observeForever {
                reviewList.clear()
                reviewList.addAll(it)
            }

            // WHEN
            viewModel.start()

            // THEN
            assertThat(reviewList).isEqualTo(reviews2)
        }

    @Test
    fun `given fetch review error and notification error, when view model started, then data from cache set once`() =
        testBlocking {
            // GIVEN
            val reviews2 = ProductReviewTestUtils.generateProductReviewList()
            doReturn(
                reviews,
                reviews2,
            ).whenever(reviewListRepository).getCachedProductReviews()
            doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
            doReturn(
                flowOf(
                    ReviewListRepository.FetchReviewsResult.NotificationsFetched(RequestResult.ERROR),
                    ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.ERROR),
                )
            ).whenever(reviewListRepository).fetchProductReviews(loadMore = false)

            val reviewList = ArrayList<ProductReview>()
            viewModel.reviewList.observeForever {
                reviewList.clear()
                reviewList.addAll(it)
            }

            // WHEN
            viewModel.start()

            // THEN
            assertThat(reviewList).isEqualTo(reviews)
        }

    @Test
    fun `Shows and hides review list load more progress correctly`() = testBlocking {
        doReturn(true).whenever(reviewListRepository).canLoadMore
        doReturn(flowOf(ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.SUCCESS)))
            .whenever(reviewListRepository).fetchProductReviews(loadMore = true)

        val isLoadingMore = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { isLoadingMore.add(it) }
        }

        viewModel.loadMoreReviews()
        assertThat(isLoadingMore).containsExactly(true, false)
    }

    @Test
    fun `Report has unread reviews status correctly`() = testBlocking {
        doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()

        var hasUnread = false
        viewModel.viewStateData.observeForever { old, new ->
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        viewModel.checkForUnreadReviews()
        assertTrue(hasUnread)
    }

    @Test
    fun `Refreshing reviews list handled correctly`() = testBlocking {
        doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
        doReturn(flowOf(ReviewListRepository.FetchReviewsResult.ReviewsFetched(RequestResult.SUCCESS)))
            .whenever(reviewListRepository).fetchProductReviews(loadMore = false)

        var hasUnread = false
        val isRefreshing = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { isRefreshing.add(it) }
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        viewModel.forceRefreshReviews()
        assertThat(isRefreshing).containsExactly(true, false)
        assertTrue(hasUnread)
    }

    @Test
    fun `Marking all reviews as read while offline handled correctly`() = testBlocking {
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.markAllReviewsAsRead()
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Notify UI that request to mark all as read was successful`() = testBlocking {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(MarkAllReviewsAsSeen.Success).whenever(markAllReviewsAsSeen).invoke()

        val markReadActions = mutableListOf<ActionStatus>()
        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            when (it) {
                is ShowSnackbar -> snackbar = it
                is MarkAllAsRead -> markReadActions.add(it.status)
            }
        }

        viewModel.markAllReviewsAsRead()

        assertThat(markReadActions).containsExactly(ActionStatus.SUBMITTED, ActionStatus.SUCCESS)
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.wc_mark_all_read_success))
    }

    @Test
    fun `Notify UI that request to mark all as read failed`() = testBlocking {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(MarkAllReviewsAsSeen.Fail).whenever(markAllReviewsAsSeen).invoke()

        val markReadActions = mutableListOf<ActionStatus>()
        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            when (it) {
                is ShowSnackbar -> snackbar = it
                is MarkAllAsRead -> markReadActions.add(it.status)
            }
        }

        viewModel.markAllReviewsAsRead()

        assertThat(markReadActions).containsExactly(ActionStatus.SUBMITTED, ActionStatus.ERROR)
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.wc_mark_all_read_error))
    }

    @Test
    fun `Given unread filter is enabled, when fetching reviews, then skeleton is shown and then hidden`() =
        testBlocking {
            doReturn(true).whenever(networkStatus).isConnected()
            doReturn(RequestResult.SUCCESS).whenever(reviewListRepository)
                .fetchOnlyUnreadProductReviews(loadMore = false)
            doReturn(emptyList<ProductReview>()).whenever(reviewListRepository).getCachedUnreadProductReviews()

            val skeletonShown = mutableListOf<Boolean>()
            viewModel.viewStateData.observeForever { old, new ->
                new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
            }

            viewModel.onUnreadReviewsFilterChanged(isEnabled = true)

            assertThat(skeletonShown).containsExactly(true, false)
        }

    @Test
    fun `Given unread filter is enabled, when fetching reviews, then unread reviews are fetched`() = testBlocking {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(RequestResult.SUCCESS).whenever(reviewListRepository).fetchOnlyUnreadProductReviews(loadMore = false)
        doReturn(emptyList<ProductReview>()).whenever(reviewListRepository).getCachedUnreadProductReviews()

        val skeletonShown = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
        }

        viewModel.onUnreadReviewsFilterChanged(isEnabled = true)

        verify(reviewListRepository, times(1)).fetchOnlyUnreadProductReviews(loadMore = false)
        verify(reviewListRepository, times(1)).getCachedUnreadProductReviews()
    }
}
