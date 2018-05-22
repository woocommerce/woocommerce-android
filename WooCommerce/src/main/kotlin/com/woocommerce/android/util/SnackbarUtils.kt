package com.woocommerce.android.util

import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.Callback
import android.view.View
import android.view.View.OnClickListener
import com.woocommerce.android.R

object SnackbarUtils {
    fun showUndoSnack(view: View, msg: String, actionListener: OnClickListener, callback: Callback? = null) {
        showSnackbarWithAction(view, msg, view.context.getString(R.string.undo), actionListener, callback)
    }

    fun getRetrySnack(view: View, msg: String, actionListener: OnClickListener, callback: Callback? = null): Snackbar {
        return getIndefiniteSnackbarWithAction(
                view, msg, view.context.getString(R.string.retry), actionListener, callback)
    }

    fun showSnack(view: View, msg: String) {
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
    }

    private fun getIndefiniteSnackbarWithAction(
        view: View,
        msg: String,
        actionString:
        String,
        actionListener: View.OnClickListener,
        callback: Snackbar.Callback? = null
    ): Snackbar {
        val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE).setAction(actionString, actionListener)

        callback?.let {
            snackbar.addCallback(it)
        }
        return snackbar
    }

    private fun showSnackbarWithAction(
        view: View,
        msg: String,
        actionString:
        String,
        actionListener: View.OnClickListener,
        callback: Snackbar.Callback? = null
    ) {
        val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setAction(actionString, actionListener)

        callback?.let {
            snackbar.addCallback(it)
        }
        snackbar.show()
    }
}
