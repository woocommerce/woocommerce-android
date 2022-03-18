package com.woocommerce.android.ui.reviews

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.combineWith
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

fun Fragment.observeModerationStatus(
    statusLiveData: LiveData<ReviewModerationStatus>,
    uiMessageResolver: UIMessageResolver,
    undoAction: () -> Unit
) {
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

fun LiveData<List<ProductReview>>.combineWithModerationStatus(
    statusLiveData: LiveData<ReviewModerationStatus>
): LiveData<List<ProductReview>> {
    return combineWith(statusLiveData) { list, status ->
        if (status == null) return@combineWith list.orEmpty()
        list?.map {
            if (it.remoteId == status.review.remoteId) {
                it.copy(status = status.newStatus.toString())
            } else {
                it
            }
        }?.filter {
            it.status != ProductReviewStatus.TRASH.toString() &&
                it.status != ProductReviewStatus.SPAM.toString()
        }.orEmpty()
    }
}

fun ScopedViewModel.observeModerationEvents(
    reviewModerationHandler: ReviewModerationHandler,
    reloadReviews: () -> Unit
) = launch {
    reviewModerationHandler.pendingModerationStatus
        .filter { it.actionStatus == ActionStatus.SUCCESS }
        .collect { reloadReviews() }
}
