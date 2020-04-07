package com.woocommerce.android.ui.reviews

import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val reviewListRepository: ReviewListRepository = mock()
    private val dispatcher: Dispatcher = mock()
    private val selectedSite: SelectedSite = mock()
    private val savedState: SavedStateWithArgs = mock()

    private val coroutineDispatchers = CoroutineDispatchers(
            Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val reviews = ProductReviewTestUtils.generateProductReviewList()
    private lateinit var viewModel: ReviewListViewModel

    @Before
    fun setup() {
        doReturn(MutableLiveData(ViewState())).whenever(savedState).getLiveData<ViewState>(any(), any())

        viewModel = spy(
                ReviewListViewModel(
                        savedState,
                        coroutineDispatchers,
                        networkStatus,
                        dispatcher,
                        selectedSite,
                        reviewListRepository
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
    fun `Load product reviews list successfully`() = test {
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
    fun `Handle loading product reviews list while offline correctly`() = test {
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
    fun `Load product reviews list failed`() = test {
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
    fun `Show and hide review list skeleton correctly`() = test {
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
    fun `Shows and hides review list load more progress correctly`() = test {
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
    fun `Report has unread reviews status correctly`() = test {
        doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()

        var hasUnread = false
        viewModel.viewStateData.observeForever { old, new ->
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        viewModel.checkForUnreadReviews()
        assertTrue(hasUnread)
    }

    @Test
    fun `Refreshing reviews list handled correctly`() = test {
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
    fun `Marking all reviews as read while offline handled correctly`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.markAllReviewsAsRead()
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Notify UI that request to mark all as read was successful`() = test {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(RequestResult.SUCCESS).whenever(reviewListRepository).markAllProductReviewsAsRead()

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
    fun `Notify UI that request to mark all as read failed`() = test {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(RequestResult.ERROR).whenever(reviewListRepository).markAllProductReviewsAsRead()

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
