package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ProductReviewStatus.HOLD
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineExceptionHandler
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.createTestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

class ReviewModerationHandlerTests : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock() {
        on { get() } doReturn SiteModel()
    }
    private val productStore: WCProductStore = mock()

    private val review = ProductReviewTestUtils.generateProductReview(0L, 0L)

    private lateinit var handler: ReviewModerationHandler

    suspend fun setup(initMocks: suspend () -> Unit = {}) {
        initMocks()
        handler = ReviewModerationHandler(
            selectedSite = selectedSite,
            productStore = productStore,
            appCoroutineScope = createTestCoroutineScope(TestCoroutineDispatcher() + TestCoroutineExceptionHandler() + coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when moderating a review, then start with a pending state`() = testBlocking {
        setup()

        val status = runTestAndCollectLastStatus {
            handler.postModerationRequest(review, HOLD)
        }

        assertThat(status.actionStatus).isEqualTo(ActionStatus.PENDING)
        assertThat(status.review).isEqualTo(review)
    }

    @Test
    fun `given moderating a review, when the undo delay is passed, then change status to submitted`() = testBlocking {
        setup()

        val latestStatus = runTestAndCollectLastStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        }

        assertThat(latestStatus.actionStatus).isEqualTo(ActionStatus.SUBMITTED)
    }

    @Test
    fun `given moderating a review, when the undo delay is passed, then submit status to the API`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WCProductReviewModel(0).apply {
                        remoteProductReviewId = this@ReviewModerationHandlerTests.review.remoteId
                        status = HOLD.toString()
                    }
                )
            )
        }

        val latestStatus = runTestAndCollectLastStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        }

        assertThat(latestStatus.actionStatus).isEqualTo(ActionStatus.SUCCESS)
    }

    @Test
    fun `when review status update succeeds, then status removed after a given time`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WCProductReviewModel(0).apply {
                        remoteProductReviewId = this@ReviewModerationHandlerTests.review.remoteId
                        status = HOLD.toString()
                    }
                )
            )
        }

        val statusList = runTestAndReturnLastEmittedStatusList {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
            advanceTimeBy(ReviewModerationHandler.SUCCESS_DELAY)
        }

        assertThat(statusList).isEmpty()
    }

    @Test
    fun `when review status update fails, then change status to error`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WooError(GENERIC_ERROR, UNKNOWN, "")
                )
            )
        }

        val latestStatus = runTestAndCollectLastStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        }

        assertThat(latestStatus.actionStatus).isEqualTo(ActionStatus.ERROR)
    }

    @Test
    fun `when review status update fails, then status removed after a given time`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WooError(GENERIC_ERROR, UNKNOWN, "")
                )
            )
        }

        val statusList = runTestAndReturnLastEmittedStatusList {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
            advanceTimeBy(ReviewModerationHandler.ERROR_SNACKBAR_DELAY)
        }

        assertThat(statusList).isEmpty()
    }

    @Test
    fun `when moderation is canceled, then submit success with original status`() = testBlocking {
        setup()

        val latestStatus = runTestAndCollectLastStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY / 2)
            handler.undoOperation(review)
        }

        assertThat(latestStatus.actionStatus).isEqualTo(ActionStatus.SUCCESS)
        assertThat(latestStatus.review.status).isEqualTo(review.status)
    }

    @Test
    fun `when queuing a second moderation request, then skip delay for the previous one`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WCProductReviewModel(0).apply {
                        remoteProductReviewId = this@ReviewModerationHandlerTests.review.remoteId
                        status = HOLD.toString()
                    }
                )
            )
        }
        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)

        val statusList = runTestAndCollectAllStatuses {
            handler.postModerationRequest(review1, HOLD)
            handler.postModerationRequest(review2, HOLD)
        }

        val statusForReview1 = statusList.last { it.review == review1 }
        assertThat(statusForReview1.actionStatus).isEqualTo(ActionStatus.SUCCESS)
    }

    @Test
    fun `when adding a second moderation request, then queue it after the previous one`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WCProductReviewModel(0).apply {
                        remoteProductReviewId = this@ReviewModerationHandlerTests.review.remoteId
                        status = HOLD.toString()
                    }
                )
            )
        }
        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)

        val statusList = runTestAndCollectAllStatuses {
            handler.postModerationRequest(review1, HOLD)
            handler.postModerationRequest(review2, HOLD)
        }

        val statusForReview2 = statusList.last { it.review == review2 }
        assertThat(statusForReview2.actionStatus).isEqualTo(ActionStatus.PENDING)
    }

    @Test
    fun `when queuing moderations, then make sure the pending status list is ordered`() = testBlocking {
        setup {
            whenever(productStore.updateProductReviewStatus(any(), any(), any())).thenReturn(
                WooResult(
                    WCProductReviewModel(0).apply {
                        remoteProductReviewId = this@ReviewModerationHandlerTests.review.remoteId
                        status = HOLD.toString()
                    }
                )
            )
        }
        val review1 = review.copy(remoteId = 1L)
        val review2 = review.copy(remoteId = 2L)

        val statusList = runTestAndReturnLastEmittedStatusList {
            handler.postModerationRequest(review1, HOLD)
            handler.postModerationRequest(review2, HOLD)
        }

        assertThat(statusList).isSorted
    }

    private suspend fun runTestAndReturnLastEmittedStatusList(
        operation: suspend () -> Unit
    ): List<ReviewModerationStatus>? {
        return coroutineScope {
            var statuses: List<ReviewModerationStatus>? = null
            val job = handler.pendingModerationStatus
                .onEach { statuses = it }
                .launchIn(this)

            launch {
                operation()
            }

            job.cancel()
            return@coroutineScope statuses
        }
    }

    private suspend fun runTestAndCollectLastStatus(operation: suspend () -> Unit) =
        runTestAndCollectAllStatuses(operation).last()

    private suspend fun runTestAndCollectAllStatuses(operation: suspend () -> Unit): List<ReviewModerationStatus> =
        coroutineScope {
            val statuses = mutableListOf<ReviewModerationStatus>()
            val job = handler.pendingModerationStatus
                .onEach { statuses.addAll(it) }
                .launchIn(this)

            launch {
                operation()
            }

            job.cancel()
            return@coroutineScope statuses
        }
}
