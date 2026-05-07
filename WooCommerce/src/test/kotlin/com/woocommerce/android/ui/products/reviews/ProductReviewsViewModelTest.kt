package com.woocommerce.android.ui.products.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.ProductReviewTestUtils
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.ui.reviews.ReviewModerationHandler
import com.woocommerce.android.ui.reviews.domain.SupportsReviewsReadStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProductReviewsViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val reviewModerationHandler: ReviewModerationHandler = mock {
        on { pendingModerationStatus } doReturn emptyFlow()
    }
    private val reviewListRepository: ReviewListRepository = mock()
    private val supportsReviewsReadStatus: SupportsReviewsReadStatus = mock()

    private lateinit var viewModel: ProductReviewsViewModel

    @Before
    fun setup() = testBlocking {
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(supportsReviewsReadStatus.invoke()).thenReturn(true)
        doReturn(emptyList<ProductReview>()).whenever(reviewListRepository).getCachedProductReviews(anyOrNull())
        doReturn(emptyFlow<ReviewListRepository.FetchReviewsResult>())
            .whenever(reviewListRepository).fetchProductReviews(loadMore = false, remoteProductId = REMOTE_PRODUCT_ID)
    }

    @Test
    fun `given read-status is unsupported, when view model is created, then unread filter is hidden`() = testBlocking {
        whenever(supportsReviewsReadStatus.invoke()).thenReturn(false)

        createViewModel()

        assertThat(viewModel.productReviewsViewStateData.liveData.value?.isUnreadFilterSupported).isFalse()
        assertThat(viewModel.productReviewsViewStateData.liveData.value?.isUnreadFilterVisible).isFalse()
    }

    @Test
    fun `given read-status is supported and reviews are available, when view model is created, then unread filter is visible`() =
        testBlocking {
            doReturn(ProductReviewTestUtils.generateProductReviewList())
                .whenever(reviewListRepository).getCachedProductReviews(anyOrNull())
            whenever(supportsReviewsReadStatus.invoke()).thenReturn(true)

            createViewModel()

            assertThat(viewModel.productReviewsViewStateData.liveData.value?.isUnreadFilterSupported).isTrue()
            assertThat(viewModel.productReviewsViewStateData.liveData.value?.isUnreadFilterVisible).isTrue()
        }

    private fun createViewModel(
        savedState: androidx.lifecycle.SavedStateHandle =
            ProductReviewsFragmentArgs(remoteProductId = REMOTE_PRODUCT_ID).toSavedStateHandle()
    ) {
        viewModel = ProductReviewsViewModel(
            savedState = savedState,
            networkStatus = networkStatus,
            reviewModerationHandler = reviewModerationHandler,
            reviewListRepository = reviewListRepository,
            supportsReviewsReadStatus = supportsReviewsReadStatus
        )
    }

    private companion object {
        const val REMOTE_PRODUCT_ID = 100L
    }
}
