package com.woocommerce.android.ui.reviews

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.ui.base.UIMessageResolver

interface ReviewModerationUi

fun ReviewModerationUi.observeModerationStatus(
    reviewModerationConsumer: ReviewModerationConsumer,
    uiMessageResolver: UIMessageResolver
) {
    if (this !is Fragment) error("This function can be called only on a Fragment receiver")

    var changeReviewStatusSnackbar: Snackbar? = null

    reviewModerationConsumer.pendingReviewModerationStatus.observe(viewLifecycleOwner) { statuses ->
        changeReviewStatusSnackbar?.dismiss()
        when {
            statuses.any { it.actionStatus == ActionStatus.ERROR } -> {
                // Prioritize error snackbar
                uiMessageResolver.showSnack(R.string.wc_moderate_review_error)
                return@observe
            }
            statuses.any { it.actionStatus == ActionStatus.PENDING } -> {
                // Get first pending status, this will be the oldest request, since list is ordered by time
                val status = statuses.first { it.actionStatus == ActionStatus.PENDING }
                changeReviewStatusSnackbar = uiMessageResolver.getIndefiniteActionSnack(
                    R.string.review_moderation_undo,
                    ProductReviewStatus.getLocalizedLabel(context, status.newStatus)
                        .lowercase(),
                    actionText = getString(R.string.undo),
                    actionListener = { reviewModerationConsumer.undoModerationRequest(status.review) }
                ).also {
                    it.show()
                }
            }
        }
    }

    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            changeReviewStatusSnackbar?.dismiss()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            changeReviewStatusSnackbar = null
            lifecycle.removeObserver(this)
        }
    })
}
