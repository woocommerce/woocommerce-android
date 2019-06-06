package com.woocommerce.android.ui.base

import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R

/**
 * Centralized snackbar creation and management. An implementing class could then be injected at the
 * Activity scope and reused for the duration of that context lifecycle. The benefit being that the implementing
 * class defines the snackbar root, the ease of injecting it straight into presenters for error handling without
 * having to pass directives over to the view, and ui testability.
 *
 * @see com.woocommerce.android.ui.main.MainUIMessageResolver
 */
interface UIMessageResolver {
    /**
     * Set by the implementing class. This is the root view the snackbar should be attached to. To enable
     * gesture support for the snackbar, this should be a CoordinatorLayout or a child of a CoordinatorLayout.
     */
    val snackbarRoot: ViewGroup

    /**
     * Create and return a snackbar displaying the provided message and a RETRY button.
     *
     * @param [stringResId] The string resource id of the base message
     * @param [stringArgs] Optional. One or more format argument stringArgs
     * @param [actionListener] Listener to handle the undo button click event
     */
    fun getUndoSnack(
        @StringRes stringResId: Int,
        vararg stringArgs: String = arrayOf(),
        actionListener: OnClickListener
    ): Snackbar {
        return getSnackbarWithAction(
                snackbarRoot,
                snackbarRoot.context.getString(stringResId, *stringArgs),
                snackbarRoot.context.getString(R.string.undo),
                actionListener)
    }

    /**
     * Create and return a snackbar displaying the provided message and a RETRY button.
     *
     * @param [stringResId] The string resource id of the base message
     * @param [stringArgs] Optional. One or more format argument stringArgs
     * @param [actionListener] Listener to handle the retry button click event
     */
    fun getRetrySnack(
        @StringRes stringResId: Int,
        vararg stringArgs: String = arrayOf(),
        actionListener: OnClickListener
    ): Snackbar {
        return getIndefiniteSnackbarWithAction(
                snackbarRoot,
                snackbarRoot.context.getString(stringResId, *stringArgs),
                snackbarRoot.context.getString(R.string.retry),
                actionListener)
    }

    /**
     * Create and return a snackbar with the provided message.
     *
     * @param [stringResId] The string resource id of the base message
     * @param [stringArgs] Optional. One or more format argument stringArgs
     */
    fun getSnack(@StringRes stringResId: Int, vararg stringArgs: String = arrayOf()) = Snackbar.make(
                snackbarRoot, snackbarRoot.context.getString(stringResId, *stringArgs), Snackbar.LENGTH_LONG)

    /**
     * Display a snackbar with the provided message.
     *
     * @param [msg] The message to display in the snackbar
     */
    fun showSnack(msg: String) = Snackbar.make(snackbarRoot, msg, Snackbar.LENGTH_LONG).show()

    /**
     * Display a snackbar with the provided string resource.
     *
     * @param [msgId] The resource ID of the message to display in the snackbar
     */
    fun showSnack(@StringRes msgId: Int) = Snackbar.make(snackbarRoot, msgId, Snackbar.LENGTH_LONG).show()

    /**
     * Display a generic offline message.
     */
    fun showOfflineSnack() = Snackbar
            .make(snackbarRoot, snackbarRoot.context.getString(R.string.offline_error), Snackbar.LENGTH_LONG)
            .show()

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
