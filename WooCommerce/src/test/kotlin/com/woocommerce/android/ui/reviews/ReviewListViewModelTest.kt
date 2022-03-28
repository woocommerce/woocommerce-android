package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.ui.reviews.domain.MarkAllReviewsAsSeen
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
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
    private val reviewModerationHandler: ReviewModerationHandler = mock() {
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
        doReturn(RequestResult.SUCCESS).whenever(reviewListRepository).fetchProductReviews(any())

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

        verify(reviewListRepository, times(1)).fetchProductReviews(any())
        verify(reviewListRepository, times(2)).getCachedProductReviews()
        Assertions.assertThat(reviewList).isEqualTo(reviews)
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

        verify(reviewListRepository, times(0)).fetchProductReviews(any())
        verify(reviewListRepository, times(1)).getCachedProductReviews()
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Load product reviews list failed`() = testBlocking {
        doReturn(reviews).whenever(reviewListRepository).getCachedProductReviews()
        doReturn(false).whenever(reviewListRepository).getHasUnreadCachedProductReviews()
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(RequestResult.ERROR).whenever(reviewListRepository).fetchProductReviews(any())

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

        verify(reviewListRepository, times(1)).fetchProductReviews(any())
        verify(reviewListRepository, times(1)).getCachedProductReviews()
        Assertions.assertThat(reviewList).isEqualTo(reviews)
        assertFalse(hasUnread)
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.review_fetch_error))
    }

    @Test
    fun `Show and hide review list skeleton correctly`() = testBlocking {
        doReturn(emptyList<ProductReview>()).whenever(reviewListRepository).getCachedProductReviews()
        doReturn(RequestResult.SUCCESS).whenever(reviewListRepository).fetchProductReviews(any())

        val skeletonShown = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
        }

        viewModel.start()

        Assertions.assertThat(skeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Shows and hides review list load more progress correctly`() = testBlocking {
        doReturn(true).whenever(reviewListRepository).canLoadMore
        doReturn(RequestResult.SUCCESS).whenever(reviewListRepository).fetchProductReviews(any())

        val isLoadingMore = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { isLoadingMore.add(it) }
        }

        viewModel.loadMoreReviews()
        Assertions.assertThat(isLoadingMore).containsExactly(true, false)
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
        doReturn(RequestResult.SUCCESS).whenever(reviewListRepository).fetchProductReviews(any())

        var hasUnread = false
        val isRefreshing = mutableListOf<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { isRefreshing.add(it) }
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        viewModel.forceRefreshReviews()
        Assertions.assertThat(isRefreshing).containsExactly(true, false)
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
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
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

        Assertions.assertThat(markReadActions).containsExactly(ActionStatus.SUBMITTED, ActionStatus.SUCCESS)
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.wc_mark_all_read_success))
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

        Assertions.assertThat(markReadActions).containsExactly(ActionStatus.SUBMITTED, ActionStatus.ERROR)
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.wc_mark_all_read_error))
    }
}
