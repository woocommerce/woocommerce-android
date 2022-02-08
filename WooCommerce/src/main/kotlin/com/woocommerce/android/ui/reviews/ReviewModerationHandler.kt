package com.woocommerce.android.ui.reviews

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ReviewModerationHandler @Inject constructor(
    private val productStore:WCProductStore,
    private val selectedSite: SelectedSite
): ReviewModeration.Handler {

    private val _reviewRequest = MutableStateFlow<ProductReviewModerationRequest?>(null)

    override val reviewRequest: StateFlow<ProductReviewModerationRequest?> = _reviewRequest

    override var pendingModerationRemoteReviewId: Long? = null

    override var pendingModerationNewStatus: String? = null

    override var pendingModerationRequest: ProductReviewModerationRequest? = null

    override suspend fun postProductReviewModerationRequest(event:OnRequestModerateReviewEvent) {
        _reviewRequest.value = event.request
    }

    // region Review Moderation
    override suspend fun submitReviewStatusChange(request: ProductReviewModerationRequest): OnProductReviewChanged {
        pendingModerationRemoteReviewId = request.productReview.remoteId
        pendingModerationNewStatus = request.newStatus.toString()
        val payload = WCProductStore.UpdateProductReviewStatusPayload(
            selectedSite.get(),
            request.productReview.remoteId,
            request.newStatus.toString())
        return productStore.updateProductReviewStatus(payload)

    }

    override fun resetPendingModerationVariables() {
        pendingModerationNewStatus = null
        pendingModerationRemoteReviewId = null
    }


}
