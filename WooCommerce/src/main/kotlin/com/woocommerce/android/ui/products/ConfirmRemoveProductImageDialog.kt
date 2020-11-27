package com.woocommerce.android.ui.products

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.R.style

class ConfirmRemoveProductImageDialog(
    context: Context,
    private val onPositiveButton: () -> Unit,
    private val onNegativeButton: () -> Unit
) : MaterialAlertDialogBuilder(ContextThemeWrapper(context, style.Theme_Woo_Dialog)) {
    init {
        setMessage(R.string.product_image_remove_confirmation)
                .setCancelable(true)
                .setPositiveButton(R.string.remove) { _, _ ->
                    onPositiveButton()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    onNegativeButton()
                }
    }
}
