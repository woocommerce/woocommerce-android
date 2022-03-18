package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ProductReviewStatus.HOLD
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
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
            appCoroutineScope = TestCoroutineScope(coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when moderating a review, then start with a pending state`() = testBlocking {
        setup()

        val status = runTestAndReadStatus {
            handler.postModerationRequest(review, HOLD)
        }

        assertThat(status?.actionStatus).isEqualTo(ActionStatus.PENDING)
        assertThat(status?.review).isEqualTo(review)
    }

    @Test
    fun `given moderating a review, when the undo delay is passed, then change status to submitted`() = testBlocking {
        setup()

        val latestStatus = runTestAndReadStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        }

        assertThat(latestStatus?.actionStatus).isEqualTo(ActionStatus.SUBMITTED)
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

        val latestStatus = runTestAndReadStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        }

        assertThat(latestStatus?.actionStatus).isEqualTo(ActionStatus.SUCCESS)
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

        val latestStatus = runTestAndReadStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY)
        }

        assertThat(latestStatus?.actionStatus).isEqualTo(ActionStatus.ERROR)
    }

    @Test
    fun `when moderation is canceled, then submit success with original status`() = testBlocking {
        setup()

        val latestStatus = runTestAndReadStatus {
            handler.postModerationRequest(review, HOLD)
            advanceTimeBy(ReviewModerationHandler.UNDO_DELAY / 2)
            handler.undoOperation(review)
        }

        assertThat(latestStatus?.actionStatus).isEqualTo(ActionStatus.SUCCESS)
        assertThat(latestStatus?.review?.status).isEqualTo(review.status)
    }

    private suspend fun runTestAndReadStatus(operation: suspend () -> Unit) =
        runTestAndReadStatuses(operation).lastOrNull()

    private suspend fun runTestAndReadStatuses(operation: suspend () -> Unit): List<ReviewModerationStatus> =
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
