package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.NavigateBackFromNotification
import com.woocommerce.android.ui.reviews.domain.MarkReviewAsSeen
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
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
            reviewModerationHandler
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

        Assertions.assertThat(skeletonShown).containsExactly(true, false)
        Assertions.assertThat(productReview).isEqualTo(review)
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

        Assertions.assertThat(skeletonShown).containsExactly(true, false)
        Assertions.assertThat(productReview).isEqualTo(review)
        Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.wc_load_review_error))
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
            Assertions.assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
        }

    @Test
    fun `Given review detail opened from notification, when navigating back, trigger NavigateBackFromNotification`() {
        doReturn(false).whenever(networkStatus).isConnected()
        viewModel.start(REVIEW_ID, launchedFromNotification = true)

        viewModel.onBackPressed()

        Assertions.assertThat(viewModel.event.value).isEqualTo(NavigateBackFromNotification)
    }

    @Test
    fun `Given review detail not opened from notification, when navigating back, trigger Exit`() {
        doReturn(false).whenever(networkStatus).isConnected()
        viewModel.start(REVIEW_ID, launchedFromNotification = false)

        viewModel.onBackPressed()

        Assertions.assertThat(viewModel.event.value).isEqualTo(Exit)
    }
}
