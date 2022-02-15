package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.ReloadReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.RemoveHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowOffLineError
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError
import com.woocommerce.android.ui.reviews.ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayReloadReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRemoveHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRemoveProductReviewFromList
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayResetPendingModerationState
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayRevertHiddenReviews
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelaySetUpModerationUndo
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayShowError
import com.woocommerce.android.ui.reviews.ReviewModeration.Relay.ReviewModerationRelayEvent.RelayToggleRefresh
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ReviewListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val reviewListRepository: ReviewListRepository = mock()
    private val dispatcher: Dispatcher = mock()

    private val savedState: SavedStateHandle = SavedStateHandle()
    private val reviews = ProductReviewTestUtils.generateProductReviewList()
    private lateinit var viewModel: ReviewListViewModel

    private val _actionEvent = MutableSharedFlow<ReviewModeration.Handler.ReviewModerationActionEvent>(0)
    private val _uiEvent = MutableSharedFlow<ReviewModeration.Handler.ReviewModerationUIEvent>(0)
    private val actionEvent = _actionEvent.asSharedFlow()
    private val uiEvent = _uiEvent.asSharedFlow()

    private val reviewModerationHandler: ReviewModerationHandler = mock {
        on { reviewModerationUIEvents } doReturn uiEvent
        on { reviewModerationActionEvents } doReturn actionEvent
    }

    @Before
    fun setup() {
        viewModel = spy(
            ReviewListViewModel(
                savedState,
                networkStatus,
                dispatcher,
                reviewListRepository,
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
    fun `Load product reviews list successfully`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Handle loading product reviews list while offline correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Load product reviews list failed`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Show and hide review list skeleton correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Shows and hides review list load more progress correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Report has unread reviews status correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(true).whenever(reviewListRepository).getHasUnreadCachedProductReviews()

        var hasUnread = false
        viewModel.viewStateData.observeForever { old, new ->
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { hasUnread = it }
        }

        viewModel.checkForUnreadReviews()
        assertTrue(hasUnread)
    }

    @Test
    fun `Refreshing reviews list handled correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Marking all reviews as read while offline handled correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(false).whenever(networkStatus).isConnected()

            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.markAllReviewsAsRead()
            Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
        }

    @Test
    fun `Notify UI that request to mark all as read was successful`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Notify UI that request to mark all as read failed`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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

    @Test
    fun `Relay review moderation status showoffline error event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reviewModerationHandler.submitReviewStatusChange(any(), any())).doReturn(
                _uiEvent.run {
                    _uiEvent.emit(ShowOffLineError)
                }
            )
            var errorEvent: RelayShowError? = null
            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is ReviewModeration.Relay.ReviewModerationRelayEvent.RelayShowError -> errorEvent = it
                    else -> {} // avoid warning
                }
            }

            viewModel.relaytReviewStatusChange(mock(), mock())
            verify(reviewModerationHandler, times(1)).submitReviewStatusChange(any(), any())
            assertNotNull(errorEvent)
            Assertions.assertThat(errorEvent!!.resID).isEqualTo(R.string.offline_error)
        }

    @Test
    fun `Relay review moderation status success`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reviewModerationHandler.submitReviewStatusChange(any(), any())).doReturn(
                _actionEvent.run {
                    _actionEvent.emit(RemoveHiddenReviews)
                    _actionEvent.emit(ResetPendingState)
                    _actionEvent.emit(ReloadReviews)
                }
            )

            var removeHiddenReviews: RelayRemoveHiddenReviews? = null
            var resetPendingState: RelayResetPendingModerationState? = null
            var reloadReviews: RelayReloadReviews? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelayRemoveHiddenReviews -> removeHiddenReviews = it
                    is RelayResetPendingModerationState -> resetPendingState = it
                    is RelayReloadReviews -> reloadReviews = it
                    else -> {
                    } // avoid warning
                }
            }

            viewModel.relaytReviewStatusChange(mock(), mock())

            verify(reviewModerationHandler, times(1)).submitReviewStatusChange(any(), any())
            assertNotNull(removeHiddenReviews)
            assertNotNull(resetPendingState)
            assertNotNull(reloadReviews)
        }

    @Test
    fun `Relay review moderation status error`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reviewModerationHandler.submitReviewStatusChange(any(), any())).doReturn(
                _actionEvent.run {
                    _actionEvent.emit(ResetPendingState)
                }.also {
                    _uiEvent.emit(ShowResponseError)
                    _uiEvent.emit(ShowRefresh(false))
                }
            )

            var showResponseError: RelayShowError? = null
            var resetPendingState: RelayResetPendingModerationState? = null
            var refresh: RelayToggleRefresh? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelayShowError -> showResponseError = it
                    is RelayResetPendingModerationState -> resetPendingState = it
                    is RelayToggleRefresh -> refresh = it
                    else -> {
                    } // avoid warning
                }
            }

            viewModel.relaytReviewStatusChange(mock(), mock())
            verify(reviewModerationHandler, times(1)).submitReviewStatusChange(any(), any())
            assertNotNull(showResponseError)
            assertNotNull(resetPendingState)
            assertNotNull(refresh)
            Assertions.assertThat(refresh!!.isRefreshing).isEqualTo(false)
            Assertions.assertThat(showResponseError!!.resID).isEqualTo(R.string.wc_moderate_review_error)
        }

    @Test
    fun `Relay review moderation status error for spam or trash`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reviewModerationHandler.submitReviewStatusChange(any(), any())).doReturn(
                _actionEvent.run {
                    _actionEvent.emit(ResetPendingState)
                    _actionEvent.emit(RevertHiddenReviews)
                }.also {
                    _uiEvent.emit(ShowResponseError)
                    _uiEvent.emit(ShowRefresh(false))
                }
            )

            var showResponseError: RelayShowError? = null
            var resetPendingState: RelayResetPendingModerationState? = null
            var refresh: RelayToggleRefresh? = null
            var removeHiddenReview: RelayRevertHiddenReviews? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelayShowError -> showResponseError = it
                    is RelayResetPendingModerationState -> resetPendingState = it
                    is RelayToggleRefresh -> refresh = it
                    is RelayRevertHiddenReviews -> removeHiddenReview = it
                    else -> {} // avoid warning
                }
            }

            viewModel.relaytReviewStatusChange(mock(), mock())
            verify(reviewModerationHandler, times(1)).submitReviewStatusChange(any(), any())
            assertNotNull(showResponseError)
            assertNotNull(resetPendingState)
            assertNotNull(refresh)
            assertNotNull(removeHiddenReview)
            Assertions.assertThat(refresh!!.isRefreshing).isEqualTo(false)
            Assertions.assertThat(showResponseError!!.resID).isEqualTo(R.string.wc_moderate_review_error)
        }

    @Test
    fun `Receive review moderation pending status`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val request = ProductReviewModerationRequest(reviews[0], ProductReviewStatus.HOLD)
            val productReviewmModerationEvent = OnRequestModerateReviewEvent(request)
            whenever(reviewModerationHandler.launchProductReviewModerationRequestFlow(any())).doReturn(
                _uiEvent.run {
                    _uiEvent.emit(ShowUndoUI(request))
                    _uiEvent.emit(ShowRefresh(true))
                }
            )

            var setupUndoRelay: RelaySetUpModerationUndo? = null
            var refresh: RelayToggleRefresh? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelaySetUpModerationUndo -> setupUndoRelay = it
                    is RelayToggleRefresh -> refresh = it
                    else -> {} // avoid warning
                }
            }

            reviewModerationHandler.launchProductReviewModerationRequestFlow(productReviewmModerationEvent)

            assertNotNull(setupUndoRelay)
            assertNotNull(refresh)
            Assertions.assertThat(refresh!!.isRefreshing).isEqualTo(true)
            Assertions.assertThat(setupUndoRelay!!.request).isEqualTo(request)
        }

    @Test
    fun `Receive review moderation pending status for spam or trash`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val request = ProductReviewModerationRequest(reviews[0], ProductReviewStatus.SPAM)
            val productReviewmModerationEvent = OnRequestModerateReviewEvent(request)
            whenever(reviewModerationHandler.launchProductReviewModerationRequestFlow(any())).doReturn(
                _uiEvent.run {
                    _uiEvent.emit(ShowUndoUI(request))
                    _uiEvent.emit(ShowRefresh(true))
                }.also {
                    _actionEvent.emit(RemoveProductReviewFromList(reviews[0].remoteId))
                }
            )

            var setupUndoRelay: RelaySetUpModerationUndo? = null
            var refresh: RelayToggleRefresh? = null
            var relayRemoveProductFromList: RelayRemoveProductReviewFromList? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelaySetUpModerationUndo -> setupUndoRelay = it
                    is RelayToggleRefresh -> refresh = it
                    is RelayRemoveProductReviewFromList -> relayRemoveProductFromList = it
                    else -> {} // avoid warning
                }
            }

            reviewModerationHandler.launchProductReviewModerationRequestFlow(productReviewmModerationEvent)

            assertNotNull(setupUndoRelay)
            assertNotNull(refresh)
            assertNotNull(relayRemoveProductFromList)
            Assertions.assertThat(refresh!!.isRefreshing).isEqualTo(true)
            Assertions.assertThat(setupUndoRelay!!.request).isEqualTo(request)
            Assertions.assertThat(relayRemoveProductFromList!!.remoteReviewId).isEqualTo(reviews[0].remoteId)
        }

    @Test
    fun `Relay undo review moderation`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reviewModerationHandler.undoReviewModerationAndResetState()).doReturn(
                _actionEvent.run {
                    _actionEvent.emit(ResetPendingState)
                }.also {
                    _uiEvent.emit(ShowRefresh(false))
                }
            )

            var resetPendingState: RelayResetPendingModerationState? = null
            var refresh: RelayToggleRefresh? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelayResetPendingModerationState -> resetPendingState = it
                    is RelayToggleRefresh -> refresh = it
                    else -> {} // avoid warning
                }
            }

            viewModel.relayUndoReviewModeration()

            verify(reviewModerationHandler, times(1)).undoReviewModerationAndResetState()
            assertNotNull(refresh)
            assertNotNull(resetPendingState)
            Assertions.assertThat(refresh!!.isRefreshing).isEqualTo(false)
        }

    @Test
    fun `Relay undo review moderation for spam or trash`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reviewModerationHandler.undoReviewModerationAndResetState()).doReturn(
                _actionEvent.run {
                    _actionEvent.emit(ResetPendingState)
                    _actionEvent.emit(RevertHiddenReviews)
                }.also {
                    _uiEvent.emit(ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh(false))
                }
            )

            var resetPendingState: RelayResetPendingModerationState? = null
            var refresh: RelayToggleRefresh? = null
            var revertHiddenreviews: RelayRevertHiddenReviews? = null

            viewModel.reviewModerationEvents.observeForever {
                when (it) {
                    is RelayResetPendingModerationState -> resetPendingState = it
                    is RelayToggleRefresh -> refresh = it
                    is RelayRevertHiddenReviews -> revertHiddenreviews = it
                    else -> {} // avoid warning
                }
            }

            viewModel.relayUndoReviewModeration()
            verify(reviewModerationHandler, times(1)).undoReviewModerationAndResetState()
            assertNotNull(refresh)
            assertNotNull(resetPendingState)
            assertNotNull(revertHiddenreviews)
            Assertions.assertThat(refresh!!.isRefreshing).isEqualTo(false)
        }
}
