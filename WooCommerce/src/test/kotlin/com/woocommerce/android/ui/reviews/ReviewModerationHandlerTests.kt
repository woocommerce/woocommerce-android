package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ProductReviewStatus.APPROVED
import com.woocommerce.android.ui.reviews.ProductReviewStatus.HOLD
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ProductReviewStatus.TRASH
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

@ExperimentalCoroutinesApi
class ReviewModerationHandlerTests : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val productStore: WCProductStore = mock()

    private val review = ProductReviewTestUtils.generateProductReview(0L, 0L)

    @Before
    fun setup() = runTest {
        whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
            WooResult(
                WCProductReviewModel(
                    remoteProductReviewId = RemoteId(review.remoteId),
                    status = HOLD.toString(),
                )
            )
        )
    }

    @Test
    fun `when moderating a review, then start with a pending state`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }
        runCurrent()

        // WHEN
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        // THEN
        collectJob.cancel()
        assertThat(statusUpdates).isNotEmpty()
        val pendingStatuses = statusUpdates.find { it.isNotEmpty() && it.first().actionStatus == ActionStatus.PENDING }
        assertThat(pendingStatuses).isNotNull
        assertThat(pendingStatuses!!).hasSize(1)
        assertThat(pendingStatuses.first().review).isEqualTo(review)
    }

    @Test
    fun `given moderating a review, when the undo delay is passed, then change status to submitted`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        val pendingStatuses = statusUpdates.flatten().filter { it.actionStatus == ActionStatus.PENDING }
        assertThat(pendingStatuses).hasSize(1)

        // WHEN
        statusUpdates.clear()
        advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        runCurrent()

        // THEN
        collectJob.cancel()
        val allStatuses = statusUpdates.flatten()
        val nonPendingStatuses = allStatuses.filter { it.actionStatus != ActionStatus.PENDING }
        assertThat(nonPendingStatuses).isNotEmpty()

        val successStatuses = allStatuses.filter { it.actionStatus == ActionStatus.SUCCESS }
        assertThat(successStatuses).hasSize(1)
    }

    @Test
    fun `given moderating a review, when the undo delay is passed, then submit status to the API`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        // WHEN
        advanceTimeBy(ReviewModerationHandler.UNDO_DELAY + 100)
        runCurrent()

        // THEN
        collectJob.cancel()
        val allStatuses = statusUpdates.flatten()
        val successStatus = allStatuses.find { it.actionStatus == ActionStatus.SUCCESS }
        assertThat(successStatus).isNotNull
    }

    @Test
    fun `when review status update succeeds, then status removed after a given time`() = runTest {
        // GIVEN
        val handler = createSUT()

        runCurrent()
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        advanceTimeBy(ReviewModerationHandler.UNDO_DELAY + 100)
        runCurrent()

        // WHEN
        advanceTimeBy(ReviewModerationHandler.SUCCESS_DELAY + 100)
        runCurrent()

        // THEN
        val currentStatuses = handler.pendingModerationStatus.first()
        assertThat(currentStatuses).isEmpty()
    }

    @Test
    fun `when review status update fails, then change status to error`() = runTest {
        // GIVEN
        val handler = createSUT()
        whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
            WooResult(WooError(GENERIC_ERROR, UNKNOWN, ""))
        )

        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        // WHEN
        advanceTimeBy(ReviewModerationHandler.UNDO_DELAY + 100)
        runCurrent()

        // THEN
        collectJob.cancel()
        val allStatuses = statusUpdates.flatten()
        val errorStatus = allStatuses.find { it.actionStatus == ActionStatus.ERROR }
        assertThat(errorStatus).isNotNull
    }

    @Test
    fun `when review status update fails, then status removed after a given time`() = runTest {
        // GIVEN
        val handler = createSUT()
        whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
            WooResult(WooError(GENERIC_ERROR, UNKNOWN, ""))
        )

        runCurrent()
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        advanceTimeBy(ReviewModerationHandler.UNDO_DELAY + 100)
        runCurrent()

        // WHEN
        advanceTimeBy(ReviewModerationHandler.ERROR_SNACKBAR_DELAY + 100)
        runCurrent()

        // THEN
        val currentStatuses = handler.pendingModerationStatus.first()
        assertThat(currentStatuses).isEmpty()
    }

    @Test
    fun `when moderation is canceled, then submit success with original status`() = runTest {
        // GIVEN
        val handler = createSUT()

        runCurrent()
        handler.postModerationRequest(review, HOLD)
        runCurrent()

        val pendingStatus = handler.pendingModerationStatus.first()
        assertThat(pendingStatus).hasSize(1)
        assertThat(pendingStatus.first().actionStatus).isEqualTo(ActionStatus.PENDING)

        // WHEN
        advanceTimeBy(ReviewModerationHandler.UNDO_DELAY / 2)
        handler.undoOperation(review)
        runCurrent()

        // THEN
        val finalStatus = handler.pendingModerationStatus.first()
        assertThat(finalStatus).isEmpty()
    }

    @Test
    fun `when queuing a second moderation request, then skip delay for the previous one`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()
        handler.postModerationRequest(review1, HOLD)
        advanceTimeBy(100)
        runCurrent()

        // WHEN
        handler.postModerationRequest(review2, HOLD)
        advanceTimeBy(100)
        runCurrent()

        // THEN
        collectJob.cancel()
        val allStatuses = statusUpdates.flatten()
        val review1Success = allStatuses.find {
            it.review.remoteId == 1L && it.actionStatus == ActionStatus.SUCCESS
        }
        assertThat(review1Success).isNotNull
    }

    @Test
    fun `when adding a second moderation request, then queue it after the previous one`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()

        // WHEN
        handler.postModerationRequest(review1, HOLD)
        handler.postModerationRequest(review2, HOLD)
        advanceTimeBy(100)
        runCurrent()

        // THEN
        collectJob.cancel()
        val latestStatuses = statusUpdates.last { it.isNotEmpty() }
        val review2Status = latestStatuses.find { it.review.remoteId == 2L }
        assertThat(review2Status?.actionStatus).isEqualTo(ActionStatus.PENDING)
    }

    @Test
    fun `when queuing moderations, then make sure the pending status list is ordered`() = runTest {
        // GIVEN
        val handler = createSUT()

        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)

        runCurrent()

        // WHEN
        handler.postModerationRequest(review1, HOLD)
        handler.postModerationRequest(review2, HOLD)
        advanceTimeBy(100)
        runCurrent()

        // THEN
        val currentStatuses = handler.pendingModerationStatus.first()
        if (currentStatuses.size > 1) {
            assertThat(currentStatuses).isSorted()
        }
    }

    @Test
    fun `when moderating to different review statuses, then handle each correctly`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statuses = listOf(APPROVED, SPAM, TRASH)

        for (status in statuses) {
            // GIVEN
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WCProductReviewModel(
                        remoteProductReviewId = RemoteId(review.remoteId),
                        status = status.toString(),
                    )
                )
            )

            runCurrent()

            // WHEN
            handler.postModerationRequest(review, status)
            runCurrent()
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY + 100)
            runCurrent()
            advanceTimeBy(ReviewModerationHandler.SUCCESS_DELAY + 100)
            runCurrent()

            // THEN
            val finalStatuses = handler.pendingModerationStatus.first()
            assertThat(finalStatuses).isEmpty()
        }
    }

    @Test
    fun `when posting multiple requests for same review rapidly, then only process the latest`() = runTest {
        // GIVEN
        val handler = createSUT()
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()

        // WHEN
        handler.postModerationRequest(review, HOLD)
        handler.postModerationRequest(review, APPROVED)
        handler.postModerationRequest(review, SPAM)
        runCurrent()
        advanceTimeBy(100)
        runCurrent()

        // THEN
        collectJob.cancel()
        val allStatuses = statusUpdates.flatten()
        val pendingForSameReview = allStatuses.filter {
            it.review.remoteId == review.remoteId && it.actionStatus == ActionStatus.PENDING
        }

        val latestPending = pendingForSameReview.lastOrNull()
        assertThat(latestPending?.newStatus).isEqualTo(SPAM)
    }

    @Test
    fun `when undoing operation that doesn't exist, then handle gracefully`() = runTest {
        // GIVEN
        val handler = createSUT()

        runCurrent()
        val nonExistentReview = review.copy(remoteId = 999L)

        // WHEN
        handler.undoOperation(nonExistentReview)
        runCurrent()

        // THEN
        val currentStatuses = handler.pendingModerationStatus.first()
        assertThat(currentStatuses).isEmpty()
    }

    @Test
    fun `when handling concurrent operations on different reviews, then process all correctly`() = runTest {
        // GIVEN
        val statusUpdates = mutableListOf<List<ReviewModerationStatus>>()
        val handler = createSUT()

        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)
        val review3 = review.copy(remoteId = 3L)

        val collectJob = launch {
            handler.pendingModerationStatus.collect { statuses ->
                statusUpdates.add(statuses)
            }
        }

        runCurrent()

        // WHEN
        handler.postModerationRequest(review1, APPROVED)
        handler.postModerationRequest(review2, SPAM)
        handler.postModerationRequest(review3, TRASH)
        runCurrent()
        advanceTimeBy(100)
        runCurrent()

        // THEN
        collectJob.cancel()
        val latestStatuses = statusUpdates.lastOrNull { it.isNotEmpty() } ?: emptyList()
        val pendingReviews = latestStatuses.filter { it.actionStatus == ActionStatus.PENDING }

        assertThat(pendingReviews).hasSize(3)
        assertThat(pendingReviews.map { it.review.remoteId }).containsExactlyInAnyOrder(1L, 2L, 3L)
    }

    private fun TestScope.createSUT() = ReviewModerationHandler(
        selectedSite = selectedSite,
        productStore = productStore,
        appCoroutineScope = backgroundScope,
    )
}
