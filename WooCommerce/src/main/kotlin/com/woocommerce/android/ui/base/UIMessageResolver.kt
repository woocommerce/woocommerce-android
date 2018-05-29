package com.woocommerce.android.ui.base

import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import com.woocommerce.android.R

/**
 * This class allows for a centralized and injectable way of handling UI-related messaging, for example
 * snackbar messaging. A single version should exist for the active context. An activity would extend this class
 * and provide the [snackbarRoot]. To enable swipe gestures for snackbar messaging, make sure the [snackbarRoot] is
 * a child ViewGroup inside a CoordinatorLayout.
 */
abstract class UIMessageResolver {
    abstract val snackbarRoot: ViewGroup

    fun getUndoSnack(@StringRes stringId: Int, msg: String?, actionListener: OnClickListener): Snackbar {
        return getSnackbarWithAction(
                snackbarRoot,
                snackbarRoot.context.getString(stringId, msg ?: ""),
                snackbarRoot.context.getString(R.string.undo),
                actionListener)
    }

    fun getRetrySnack(@StringRes stringId: Int, msg: String?, actionListener: OnClickListener): Snackbar {
        return getIndefiniteSnackbarWithAction(
                snackbarRoot,
                snackbarRoot.context.getString(stringId, msg ?: ""),
                snackbarRoot.context.getString(R.string.retry),
                actionListener)
    }

    fun showSnack(@StringRes stringId: Int, msg: String? = null) =
            Snackbar.make(snackbarRoot, snackbarRoot.context.getString(stringId, msg ?: ""), Snackbar.LENGTH_LONG)
                    .show()

    fun showSnack(msg: String) = Snackbar.make(snackbarRoot, msg, Snackbar.LENGTH_LONG).show()

    private fun getIndefiniteSnackbarWithAction(
        view: View,
        msg: String,
        actionString: String,
        actionListener: View.OnClickListener
    ) = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE).setAction(actionString, actionListener)

    private fun getSnackbarWithAction(
        view: View,
        msg: String,
        actionString: String,
        actionListener: OnClickListener
    ) = Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setAction(actionString, actionListener)
}
