package com.woocommerce.android.ui.main

import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject

/**
 * This class allows for a centralized and injectable way of handling UI-related messaging, for example
 * snackbar messaging. A single version should exist for the active context.
 *
 * TODO: Abstract this out to allow it to be reused by other Activities
 */
@ActivityScope
class MainUIMessageResolver @Inject constructor(val activity: MainActivity) : UIMessageResolver {
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.snack_root) as ViewGroup
    }

    override fun getUndoSnack(@StringRes stringId: Int, msg: String?, actionListener: OnClickListener): Snackbar {
        return getSnackbarWithAction(
                snackbarRoot,
                snackbarRoot.context.getString(stringId, msg ?: ""),
                snackbarRoot.context.getString(R.string.undo),
                actionListener)
    }

    override fun getRetrySnack(@StringRes stringId: Int, msg: String?, actionListener: OnClickListener): Snackbar {
        return getIndefiniteSnackbarWithAction(
                snackbarRoot,
                snackbarRoot.context.getString(stringId, msg ?: ""),
                snackbarRoot.context.getString(R.string.retry),
                actionListener)
    }

    override fun showSnack(@StringRes stringId: Int, msg: String?) =
            Snackbar.make(snackbarRoot, snackbarRoot.context.getString(stringId, msg ?: ""), Snackbar.LENGTH_LONG)
                    .show()

    override fun showSnack(msg: String) = Snackbar.make(snackbarRoot, msg, Snackbar.LENGTH_LONG).show()

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
