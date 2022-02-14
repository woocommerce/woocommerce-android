package com.woocommerce.android.ui.reviews

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class ReviewModerationHandlerTest : BaseUnitTest() {
    private lateinit var reviewHandler: ReviewModerationHandler
    private val networkStatus: NetworkStatus = mock()
    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val review = ProductReviewTestUtils.generateProductReview(
        id = REVIEW_ID,
        productId = PRODUCT_ID
    )

    companion object {
        const val REVIEW_ID = 1L
        const val PRODUCT_ID = 200L
    }


    @Before
    fun setup() {
        reviewHandler = spy(
            ReviewModerationHandler(
                productStore,
                selectedSite,
                networkStatus
            )
        )
    }

    @Test
    fun `emit offline error for submit review status change`()
    = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(false).whenever(networkStatus).isConnected()
        var offlineErrorEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowOffLineError? = null
        val job = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                if(it is ReviewModeration.Handler.ReviewModerationUIEvent.ShowOffLineError ) offlineErrorEvent = it
            }
        }

        reviewHandler.submitReviewStatusChange(mock(),mock())

        job.cancel()
        assertNotNull(offlineErrorEvent)
    }

    @Test
    fun `emit events for submit review status change action success`()
    = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(true).whenever(networkStatus).isConnected()
        val onProductreviewChanged = WCProductStore.OnProductReviewChanged(1)
        doReturn(onProductreviewChanged).whenever(productStore).updateProductReviewStatus(any())
        doReturn(SiteModel()).whenever(selectedSite).get()
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var removeHiddenReviews: ReviewModeration.Handler.ReviewModerationActionEvent.RemoveHiddenReviews? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null
        var reloadReviews: ReviewModeration.Handler.ReviewModerationActionEvent.ReloadReviews? = null

        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                if(it is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh ) refreshEvent = it
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveHiddenReviews -> removeHiddenReviews = it
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ReloadReviews -> reloadReviews = it
                    else -> {}
                }
            }
        }

        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.HOLD)))
        reviewHandler.submitReviewStatusChange(mock(),mock())

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(removeHiddenReviews)
        assertNotNull(resetPendingSate)
        assertNotNull(reloadReviews)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }

    @Test
    fun `emit events for submit review status change action error`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(true).whenever(networkStatus).isConnected()
        var responseError: ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError? = null
        val onProductreviewChanged = WCProductStore.OnProductReviewChanged(0).also { it.error = mock() }
        doReturn(onProductreviewChanged).whenever(productStore).updateProductReviewStatus(any())
        doReturn(SiteModel()).whenever(selectedSite).get()
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null

        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                when(it){
                    is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> refreshEvent = it
                    is  ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError -> responseError = it
                    else -> {}
                }
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    else -> {}
                }
            }
        }

        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.APPROVED)))
        reviewHandler.submitReviewStatusChange(mock(),mock())

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(resetPendingSate)
        assertNotNull(responseError)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }

    @Test
    fun `emit events for submit review status change action error for trash`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(true).whenever(networkStatus).isConnected()
        var responseError: ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError? = null
        val onProductreviewChanged = WCProductStore.OnProductReviewChanged(0).also { it.error = mock() }
        doReturn(onProductreviewChanged).whenever(productStore).updateProductReviewStatus(any())
        doReturn(SiteModel()).whenever(selectedSite).get()
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null
        var revertHiddenreviews:ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews? = null

        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                when(it){
                    is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> refreshEvent = it
                    is  ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError -> responseError = it
                    else -> {}
                }
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews -> revertHiddenreviews = it
                    else -> {}
                }
            }
        }

        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.TRASH)))
        reviewHandler.submitReviewStatusChange(review,ProductReviewStatus.SPAM)

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(resetPendingSate)
        assertNotNull(responseError)
        assertNotNull(revertHiddenreviews)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }

    @Test
    fun `emit events for submit review status change action error for spam`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(true).whenever(networkStatus).isConnected()
        var responseError: ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError? = null
        val onProductreviewChanged = WCProductStore.OnProductReviewChanged(0).also { it.error = mock() }
        doReturn(onProductreviewChanged).whenever(productStore).updateProductReviewStatus(any())
        doReturn(SiteModel()).whenever(selectedSite).get()
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null
        var revertHiddenreviews:ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews? = null


        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                when(it){
                    is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> refreshEvent = it
                    is  ReviewModeration.Handler.ReviewModerationUIEvent.ShowResponseError -> responseError = it
                    else -> {}
                }
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews -> revertHiddenreviews = it
                    else -> {}
                }
            }
        }

        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.SPAM)))
        reviewHandler.submitReviewStatusChange(mock(),mock())

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(resetPendingSate)
        assertNotNull(responseError)
        assertNotNull(revertHiddenreviews)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }

    @Test
    fun `emit events for launch review flow with action pending`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var showUndoUI: ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI? = null


        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                when(it){
                    is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> refreshEvent = it
                    is  ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI -> showUndoUI = it
                    else -> {}
                }
            }
        }


        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.APPROVED)))

        job1.cancel()
        assertNotNull(refreshEvent)
        assertNotNull(showUndoUI)
        assertNotNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, true)
        assertEquals(showUndoUI!!.productReviewModerationRequest.productReview.remoteId, review.remoteId)

    }

    @Test
    fun `emit events for launch review flow with action pending for spam`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var showUndoUI: ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI? = null
        var removeReviewFromList: ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList? = null


        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                when(it){
                    is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> refreshEvent = it
                    is  ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI -> showUndoUI = it
                    else -> {}
                }
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList -> removeReviewFromList = it
                    else -> {}
                }
            }
        }


        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.SPAM)))

        job1.cancel()
        job2.cancel()
        assertNotNull(refreshEvent)
        assertNotNull(showUndoUI)
        assertNotNull(removeReviewFromList)
        assertNotNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, true)
        assertEquals(showUndoUI!!.productReviewModerationRequest.productReview.remoteId, review.remoteId)
        assertEquals(removeReviewFromList!!.remoteReviewId, review.remoteId)

    }

    @Test
    fun `emit events for launch review flow with action pending for trash`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var showUndoUI: ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI? = null
        var removeReviewFromList: ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList? = null


        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                when(it){
                    is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh -> refreshEvent = it
                    is  ReviewModeration.Handler.ReviewModerationUIEvent.ShowUndoUI -> showUndoUI = it
                    else -> {}
                }
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RemoveProductReviewFromList -> removeReviewFromList = it
                    else -> {}
                }
            }
        }


        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.TRASH)))

        job1.cancel()
        job2.cancel()
        assertNotNull(refreshEvent)
        assertNotNull(showUndoUI)
        assertNotNull(removeReviewFromList)
        assertNotNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, true)
        assertEquals(showUndoUI!!.productReviewModerationRequest.productReview.remoteId, review.remoteId)
        assertEquals(removeReviewFromList!!.remoteReviewId, review.remoteId)

    }

    @Test
    fun `emit events for undo moderation for spam`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var revertHiddenReviews: ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null



        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                if(it is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh ) refreshEvent = it
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews -> revertHiddenReviews = it
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    else -> {}
                }
            }
        }
        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.SPAM)))


        reviewHandler.undoReviewModerationAndResetState()

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(revertHiddenReviews)
        assertNotNull(resetPendingSate)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }

    @Test
    fun `emit events for undo moderation for trash`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var revertHiddenReviews: ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null



        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                if(it is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh ) refreshEvent = it
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.RevertHiddenReviews -> revertHiddenReviews = it
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    else -> {}
                }
            }
        }
        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.TRASH)))


        reviewHandler.undoReviewModerationAndResetState()

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(revertHiddenReviews)
        assertNotNull(resetPendingSate)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }

    @Test
    fun `emit events for undo moderation`()
        = coroutinesTestRule.testDispatcher.runBlockingTest {
        var refreshEvent: ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh? = null
        var resetPendingSate: ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState? = null



        val job1 = this.launch {
            reviewHandler.getReviewModerationUiEventFlow().collect {
                if(it is ReviewModeration.Handler.ReviewModerationUIEvent.ShowRefresh ) refreshEvent = it
            }
        }
        val job2 = this.launch {
            reviewHandler.getReviewModerationActionEventFlow().collect{
                when(it){
                    is ReviewModeration.Handler.ReviewModerationActionEvent.ResetPendingState -> resetPendingSate = it
                    else -> {}
                }
            }
        }
        reviewHandler.launchProductReviewModerationRequestFlow(
            OnRequestModerateReviewEvent(ProductReviewModerationRequest(review, ProductReviewStatus.APPROVED)))


        reviewHandler.undoReviewModerationAndResetState()

        job1.cancel()
        job2.cancel()

        assertNotNull(refreshEvent)
        assertNotNull(resetPendingSate)
        assertNull(reviewHandler.getPendingReviewModerationRequest())
        assertEquals(refreshEvent!!.isRefreshing, false)
    }


}
