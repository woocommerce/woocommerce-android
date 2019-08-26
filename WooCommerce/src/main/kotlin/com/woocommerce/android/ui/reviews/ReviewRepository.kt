package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

final class ReviewRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuationReview: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProductReviews(): List<ProductReview> {
        suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
            continuationReview = it

            val payload = WCProductStore.FetchProductReviewsPayload(selectedSite.get())
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
        }

        return getProductReviews()
    }

    fun getProductReviews(): List<ProductReview> =
            productStore.getProductReviewsForSite(selectedSite.get()).map { it.toAppModel() }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_REVIEWS) {
            if (event.isError) {
                continuationReview?.resume(false)
            } else {
                // TODO track product reviews loaded

                continuationReview?.resume(true)
            }
        }
    }
}
