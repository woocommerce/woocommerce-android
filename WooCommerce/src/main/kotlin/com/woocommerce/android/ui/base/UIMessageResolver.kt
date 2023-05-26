package com.woocommerce.android.ui.base

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString

/**
 * Centralized snackbar creation and management. An implementing class could then be injected at the
 * Activity scope and reused for the duration of that context lifecycle. The benefit being that the implementing
 * class defines the snackbar root, the ease of injecting it straight into presenters for error handling without
 * having to pass directives over to the view, and ui testability.
 *
 * @see com.woocommerce.android.ui.message.DefaultUIMessageResolver
 */
interface UIMessageResolver {
    /**
     * Set by the implementing class. This is the root view the snackbar should be attached to. To enable
     * gesture support for the snackbar, this should be a CoordinatorLayout or a child of a CoordinatorLayout.
     */
    val snackbarRoot: ViewGroup

    var anchorViewId: Int?

    /**
     * Create and return a snackbar displaying a message to restart the app once the in app update has been
     * successfully installed
     *
     * @param [stringResId] The string resource id of the base message
     * @param [stringArgs] Optional. One or more format argument stringArgs
     * @param [actionListener] Listener to handle the install button click event
     */
    fun getRestartSnack(
        @StringRes stringResId: Int,
        vararg stringArgs: String = arrayOf(),
        actionListener: View.OnClickListener
    ): Snackbar {
        return getIndefiniteSnackbarWithAction(
            snackbarRoot,
            snackbarRoot.context.getString(stringResId, *stringArgs),
            snackbarRoot.context.getString(R.string.install),
            actionListener,
            anchorViewId
        )
    }

    /**
     * Create and return a snackbar displaying the provided message and a generic action button.
     *
     * @param [message] The message string
     * @param [message] The action string
     * @param [stringArgs] Optional. One or more format argument stringArgs
     * @param [actionListener] Listener to handle the undo button click event
     */
    fun getIndefiniteActionSnack(
        message: String,
        vararg stringArgs: String = arrayOf(),
        actionText: String,
        actionListener: View.OnClickListener
    ): Snackbar {
        return getIndefiniteSnackbarWithAction(
            snackbarRoot,
            String.format(message, *stringArgs),
            actionText,
            actionListener,
            anchorViewId
        )
    }

    /**
     * Create and return a snackbar displaying the provided message and a generic action button.
     *
     * @param [message] The message string
     * @param [message] The action string
     * @param [stringArgs] Optional. One or more format argument stringArgs
     * @param [actionListener] Listener to handle the undo button click event
     */
    fun getIndefiniteActionSnack(
        @StringRes message: Int,
        vararg stringArgs: String = arrayOf(),
        actionText: String,
        actionListener: View.OnClickListener
    ): Snackbar {
        return getIndefiniteSnackbarWithAction(
            snackbarRoot,
            snackbarRoot.context.getString(message, *stringArgs),
            actionText,
            actionListener,
            anchorViewId
        )
    }

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
        actionListener: View.OnClickListener
    ): Snackbar {
        return getSnackbarWithAction(
            snackbarRoot,
            snackbarRoot.context.getString(stringResId, *stringArgs),
            snackbarRoot.context.getString(R.string.undo),
            actionListener,
            anchorViewId
        )
    }

    /**
     * Create and return a snackbar displaying the provided message and a RETRY button.
     *
     * @param [message] The message string
     * @param [stringArgs] Optional. One or more format argument stringArgs
     * @param [actionListener] Listener to handle the undo button click event
     */
    fun getUndoSnack(
        message: String,
        vararg stringArgs: String = arrayOf(),
        actionListener: View.OnClickListener
    ): Snackbar {
        return getSnackbarWithAction(
            snackbarRoot,
            String.format(message, *stringArgs),
            snackbarRoot.context.getString(R.string.undo),
            actionListener,
            anchorViewId
        )
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
        actionListener: View.OnClickListener
    ): Snackbar {
        return getIndefiniteSnackbarWithAction(
            snackbarRoot,
            snackbarRoot.context.getString(stringResId, *stringArgs),
            snackbarRoot.context.getString(R.string.retry),
            actionListener,
            anchorViewId
        )
    }

    /**
     * Create and return a snackbar displaying the provided message and a RETRY button.
     *
     * @param [message] The message string
     * @param [actionListener] Listener to handle the retry button click event
     */
    fun getRetrySnack(
        message: String,
        actionListener: View.OnClickListener
    ): Snackbar {
        return getIndefiniteSnackbarWithAction(
            snackbarRoot,
            message,
            snackbarRoot.context.getString(R.string.retry),
            actionListener,
            anchorViewId
        )
    }

    /**
     * Create and return a snackbar with the provided message.
     *
     * @param [stringResId] The string resource id of the base message
     * @param [stringArgs] Optional. One or more format argument stringArgs
     */
    fun getSnack(@StringRes stringResId: Int, vararg stringArgs: String = arrayOf()) = Snackbar.make(
        snackbarRoot, snackbarRoot.context.getString(stringResId, *stringArgs), BaseTransientBottomBar.LENGTH_LONG
    ).apply {
        anchorViewId?.let { setAnchorView(it) }
    }

    /**
     * Display a snackbar with the provided message.
     *
     * @param [msg] The message to display in the snackbar
     */
    fun showSnack(msg: String) = Snackbar.make(snackbarRoot, msg, BaseTransientBottomBar.LENGTH_LONG)
        .apply {
            anchorViewId?.let { setAnchorView(it) }
        }
        .show()

    /**
     * Display a snackbar with the provided string resource.
     *
     * @param [msgId] The resource ID of the message to display in the snackbar
     */
    fun showSnack(@StringRes msgId: Int) = Snackbar.make(snackbarRoot, msgId, BaseTransientBottomBar.LENGTH_LONG)
        .apply {
            anchorViewId?.let { setAnchorView(it) }
        }
        .show()

    /**
     * Display a snackbar with the provided [UiString].
     *
     * @param [message] The message to display in the snackbar
     */
    fun showSnack(message: UiString) {
        val snackbar = when (message) {
            is UiString.UiStringRes ->
                Snackbar.make(snackbarRoot, message.stringRes, BaseTransientBottomBar.LENGTH_LONG)
            is UiString.UiStringText -> Snackbar.make(snackbarRoot, message.text, BaseTransientBottomBar.LENGTH_LONG)
        }.apply {
            anchorViewId?.let { setAnchorView(it) }
        }
        snackbar.show()
    }
}

private fun getIndefiniteSnackbarWithAction(
    view: View,
    msg: String,
    actionString: String,
    actionListener: View.OnClickListener,
    anchorViewId: Int?
) = Snackbar.make(view, msg, BaseTransientBottomBar.LENGTH_INDEFINITE).setAction(actionString, actionListener)
    .apply {
        anchorViewId?.let {
            setAnchorView(it)
        }
    }

private fun getSnackbarWithAction(
    view: View,
    msg: String,
    actionString: String,
    actionListener: View.OnClickListener,
    anchorViewId: Int?
) = Snackbar.make(view, msg, BaseTransientBottomBar.LENGTH_LONG).setAction(actionString, actionListener)
    .apply {
        anchorViewId?.let {
            setAnchorView(it)
        }
    }
