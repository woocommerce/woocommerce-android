package com.woocommerce.android.ui.orders.shippinglabels

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.navigateBackWithResult
import java.util.Locale

class ShippingLabelPaperSizeSelectorDialog : DialogFragment() {
    enum class ShippingLabelPaperSize(@StringRes val stringResource: Int) {
        LABEL(string.shipping_label_paper_size_label),
        LEGAL(string.shipping_label_paper_size_legal),
        LETTER(string.shipping_label_paper_size_letter);
    }

    companion object {
        const val KEY_PAPER_SIZE_RESULT = "key_paper_size_result"
    }

    private val navArgs: ShippingLabelPaperSizeSelectorDialogArgs by navArgs()
    private val paperSizeOptions: Array<String> by lazy {
        ShippingLabelPaperSize.values().map { getString(it.stringResource) }.toTypedArray()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(resources.getString(string.shipping_label_paper_size_options_title))
            .setSingleChoiceItems(paperSizeOptions, getCurrentPaperSizeIndex()) { _, which ->
                navigateBackWithResult(
                    key = KEY_PAPER_SIZE_RESULT,
                    result = ShippingLabelPaperSize.values()[which]
                )
            }
            .create()
    }

    private fun getCurrentPaperSizeIndex() = ShippingLabelPaperSize
        .values().indexOfFirst { it == navArgs.currentPaperSize }
}
