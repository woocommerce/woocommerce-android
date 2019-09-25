package com.woocommerce.android.model

/**
 * Base class for communicating the status of a user initiated request event that
 * uses the "optimistic" design by delaying the submission of a user request to the API
 * and displaying an UNDO snack message giving the user the opportunity to back out of
 * the requested action. The request will then be submitted to the API once the snackbar
 * times out, the user navigates away from the screen or app, or if the user swipes to
 * dismiss the snackbar.
 *
 * @see [com.woocommerce.android.ui.reviews.ProductReviewModerationAction] for an example
 * of how this class is implemented.
 * @see [com.woocommerce.android.ui.reviews.OnRequestModerateReviewEvent] for an example
 * of how the various status options are used to communicate the lifecycle of a request to
 * moderate a [ProductReview].
 */
abstract class DelayedUndoRequest(var requestStatus: RequestStatus) {
    enum class RequestStatus {
        PENDING {
            override fun isComplete() = false
        },
        SUBMITTED {
            override fun isComplete() = false
        },
        SUCCESS {
            override fun isComplete() = true
        },
        ERROR {
            override fun isComplete() = true
        };

        abstract fun isComplete(): Boolean
    }
}
