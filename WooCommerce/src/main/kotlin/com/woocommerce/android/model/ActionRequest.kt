package com.woocommerce.android.model

/**
 * Base class for communicating the status of a change that requires communicating
 * with the connected store to complete the request.
 *
 * @see [com.woocommerce.android.ui.reviews.ProductReviewModerationRequest] for an example
 * of how this class is implemented.
 * @see [com.woocommerce.android.ui.reviews.OnRequestModerateReviewEvent] for an example
 * of how the various status options are used to communicate the lifecycle of a request to
 * moderate a [ProductReview].
 */
abstract class ActionRequest(var actionStatus: ActionStatus)
