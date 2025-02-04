package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_REPLY_SEND
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_REPLY_SEND_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_REPLY_SEND_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.NavigateBackFromNotification
import com.woocommerce.android.ui.reviews.domain.MarkReviewAsSeen
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ReviewDetailViewModelTest : BaseUnitTest() {
    companion object {
        const val REVIEW_ID = 1L
        const val NOTIF_ID = 300L
        const val PRODUCT_ID = 200L
    }

    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val repository: ReviewDetailRepository = mock()
    private val savedState = SavedStateHandle()
    private val markReviewAsSeen: MarkReviewAsSeen = mock()
    private val reviewModerationHandler: ReviewModerationHandler = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val review = ProductReviewTestUtils.generateProductReview(id = REVIEW_ID, productId = PRODUCT_ID)
    private lateinit var viewModel: ReviewDetailViewModel
    private val notification = ProductReviewTestUtils.generateReviewNotification(NOTIF_ID)

    @Before
    fun setup() {
        viewModel = ReviewDetailViewModel(
            savedState,
            networkStatus,
            repository,
            markReviewAsSeen,
            reviewModerationHandler,
            analyticsTracker
        )
    }

    @Test
    fun `Load the product review detail correctly`() = testBlocking {
        doReturn(review).whenever(repository).getCachedProductReview(any())
        doReturn(notification).whenever(repository).getCachedNotificationForReview(any())
        doReturn(RequestResult.ERROR).whenever(repository).fetchProductReview(any())

        val skeletonShown = mutableListOf<Boolean>()
        var productReview: ProductReview? = null
        viewModel.viewStateData.observeForever { old, new ->
            new.productReview?.takeIfNotEqualTo(old?.productReview) { productReview = it }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
        }

        viewModel.start(REVIEW_ID, false)

        assertThat(skeletonShown).containsExactly(true, false)
        assertThat(productReview).isEqualTo(review)
        verify(markReviewAsSeen, times(1)).invoke(REVIEW_ID, notification)
    }

    @Test
    fun `Handle error in loading product review detail correctly`() = testBlocking {
        doReturn(notification).whenever(repository).getCachedNotificationForReview(any())
        doReturn(review).whenever(repository).getCachedProductReview(any())
        doReturn(RequestResult.ERROR).whenever(repository).fetchProductReview(any())

        val skeletonShown = mutableListOf<Boolean>()
        var productReview: ProductReview? = null
        viewModel.viewStateData.observeForever { old, new ->
            new.productReview?.takeIfNotEqualTo(old?.productReview) { productReview = it }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonShown.add(it) }
        }

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            when (it) {
                is ShowSnackbar -> snackbar = it
            }
        }

        viewModel.start(REVIEW_ID, false)

        assertThat(skeletonShown).containsExactly(true, false)
        assertThat(productReview).isEqualTo(review)
        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.wc_load_review_error))
        verify(markReviewAsSeen, times(1)).invoke(REVIEW_ID, notification)
    }

    /**
     * Verifies the `exit` LiveData event is called when a request to moderate
     * a review is processed successfully by the detail view.
     */
    @Test
    fun `Handle successful review moderation correctly`() = testBlocking {
        doReturn(notification).whenever(repository).getCachedNotificationForReview(any())
        doReturn(review).whenever(repository).getCachedProductReview(any())
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchProductReview(any())

        // first we must load the product review so the viewmodel will have
        // a reference to it.
        viewModel.start(REVIEW_ID, false)

        var exitCalled = false
        viewModel.event.observeForever {
            when (it) {
                is Exit -> exitCalled = true
            }
        }

        viewModel.moderateReview(SPAM)

        verify(reviewModerationHandler).postModerationRequest(review, newStatus = SPAM)
        assertTrue(exitCalled)
    }

    /**
     * Verifies an error message is shown when a request to moderate a review is
     * submitted while the device is offline. The `exit` LiveData event should never
     * be called.
     */
    @Test
    fun `Handle review moderation failed due to offline correctly`() =
        testBlocking {
            doReturn(false).whenever(networkStatus).isConnected()

            doReturn(review).whenever(repository).getCachedProductReview(any())

            // first we must load the product review so the viewmodel will have
            // a reference to it.
            viewModel.start(REVIEW_ID, false)

            var snackbar: ShowSnackbar? = null
            var exitCalled = false
            viewModel.event.observeForever {
                when (it) {
                    is Exit -> exitCalled = true
                    is ShowSnackbar -> snackbar = it
                }
            }

            viewModel.moderateReview(SPAM)
            assertFalse(exitCalled)
            assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
        }

    @Test
    fun `Given review detail opened from notification, when navigating back, trigger NavigateBackFromNotification`() {
        doReturn(false).whenever(networkStatus).isConnected()
        viewModel.start(REVIEW_ID, launchedFromNotification = true)

        viewModel.onBackPressed()

        assertThat(viewModel.event.value).isEqualTo(NavigateBackFromNotification)
    }

    @Test
    fun `Given review detail not opened from notification, when navigating back, trigger Exit`() {
        doReturn(false).whenever(networkStatus).isConnected()
        viewModel.start(REVIEW_ID, launchedFromNotification = false)

        viewModel.onBackPressed()

        assertThat(viewModel.event.value).isEqualTo(Exit)
    }

    @Test
    fun `When review reply button is pressed, open reply view`() {
        // given
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever(events::add)

        // when
        viewModel.onReplyClicked()

        // then
        assertThat(events).last().isEqualTo(ReviewDetailViewModel.ReviewDetailEvent.Reply)
    }

    @Test
    fun `When review reply is requested and successful, track analytics event and show snackbar`() {
        // given
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever(events::add)
        repository.stub {
            onBlocking { reply(any(), any(), any()) } doReturn WooResult()
            onBlocking { getCachedProductReview(review.remoteId) } doReturn review
        }

        // when
        viewModel.start(REVIEW_ID, false)
        viewModel.onReviewReplied("reply")

        // then
        verify(analyticsTracker).track(REVIEW_REPLY_SEND)
        verify(analyticsTracker).track(REVIEW_REPLY_SEND_SUCCESS)
        assertThat(events).last().isEqualTo(ShowSnackbar(R.string.review_reply_success))
    }

    @Test
    fun `When review reply is requested and failed, track analytics event and show snackbar`() {
        // given
        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever(events::add)
        repository.stub {
            onBlocking { reply(any(), any(), any()) } doReturn WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            onBlocking { getCachedProductReview(review.remoteId) } doReturn review
        }

        // when
        viewModel.start(REVIEW_ID, false)
        viewModel.onReviewReplied("reply")

        // then
        verify(analyticsTracker).track(REVIEW_REPLY_SEND)
        verify(analyticsTracker).track(REVIEW_REPLY_SEND_FAILED)
        assertThat(events).last().isEqualTo(ShowSnackbar(R.string.review_reply_failure))
    }

    @Test
    fun `given review opened from notification, when review is moderated, trigger NavigateBackFromNotification`() =
        testBlocking {
            doReturn(true).whenever(networkStatus).isConnected()
            doReturn(review).whenever(repository).getCachedProductReview(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchProductReview(any())
            viewModel.start(REVIEW_ID, launchedFromNotification = true)

            viewModel.moderateReview(SPAM)

            assertThat(viewModel.event.value).isEqualTo(NavigateBackFromNotification)
        }
}
