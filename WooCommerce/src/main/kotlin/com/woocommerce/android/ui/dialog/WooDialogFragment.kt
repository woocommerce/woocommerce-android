package com.woocommerce.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WooDialogFragment : DialogFragment() {

    private var dialogInteractionListener: DialogInteractionListener? = null

    fun setDialogInteractionListener(listener: DialogInteractionListener) {
        dialogInteractionListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @Suppress("DEPRECATION") val params = requireArguments().getParcelable<DialogParams>(ARG_DIALOG_PARAMS)!!

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setCancelable(params.cancelable)

        params.titleId?.let { builder.setTitle(it) }
        params.messageId?.let { builder.setMessage(it) }
        params.positiveButtonId?.let { posId ->
            builder.setPositiveButton(posId) { _, _ ->
                dialogInteractionListener?.onPositiveButtonClicked()
            }
        }
        params.negativeButtonId?.let { negId ->
            builder.setNegativeButton(negId) { _, _ ->
                dialogInteractionListener?.onNegativeButtonClicked()
            }
        }
        params.neutralButtonId?.let { neutId ->
            builder.setNeutralButton(neutId) { _, _ ->
                dialogInteractionListener?.onNeutralButtonClicked()
            }
        }

        return builder.create()
    }

    interface DialogInteractionListener {
        fun onPositiveButtonClicked()
        fun onNegativeButtonClicked()
        fun onNeutralButtonClicked()
    }

    companion object {
        const val ARG_DIALOG_PARAMS = "dialog_params"
        const val TAG = "WooDialogFragment"
        fun newInstance(params: DialogParams): WooDialogFragment {
            return WooDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_DIALOG_PARAMS, params)
                }
            }
        }
    }
}
