package com.woocommerce.android.ui.reviews

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.ui.base.UIMessageResolver

interface ReviewModerationUi

fun ReviewModerationUi.observeModerationStatus(
    statusLiveData: LiveData<ReviewModerationStatus>,
    uiMessageResolver: UIMessageResolver,
    undoAction: () -> Unit
) {
    if (this !is Fragment) error("This function can be called only on a Fragment receiver")

    var changeReviewStatusSnackbar: Snackbar? = null

    statusLiveData.observe(viewLifecycleOwner) { status ->
        changeReviewStatusSnackbar?.dismiss()
        when (status.actionStatus) {
            ActionStatus.PENDING -> {
                changeReviewStatusSnackbar = uiMessageResolver.getIndefiniteActionSnack(
                    R.string.review_moderation_undo,
                    ProductReviewStatus.getLocalizedLabel(context, status.newStatus)
                        .lowercase(),
                    actionText = getString(R.string.undo),
                    actionListener = { undoAction() }
                ).also {
                    it.show()
                }
            }
            ActionStatus.ERROR -> {
                uiMessageResolver.getSnack(R.string.wc_moderate_review_error)
                    .also { it.show() }
            }
            else -> {
                // NO-OP
            }
        }
    }

    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            changeReviewStatusSnackbar?.dismiss()
            changeReviewStatusSnackbar = null
            lifecycle.removeObserver(this)
        }
    })
}
