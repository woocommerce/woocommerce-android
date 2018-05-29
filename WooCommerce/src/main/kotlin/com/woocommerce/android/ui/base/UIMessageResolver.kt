package com.woocommerce.android.ui.base

import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View.OnClickListener
import android.view.ViewGroup

interface UIMessageResolver {
    val snackbarRoot: ViewGroup

    fun getUndoSnack(@StringRes stringId: Int, msg: String?, actionListener: OnClickListener): Snackbar

    fun getRetrySnack(@StringRes stringId: Int, msg: String?, actionListener: OnClickListener): Snackbar

    fun showSnack(@StringRes stringId: Int, msg: String? = null)

    fun showSnack(msg: String)
}
