package com.woocommerce.android.ui.login.storecreation.onboarding

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NameYourStoreDialogFragment : DialogFragment() {
    private val viewModel: StoreOnboardingViewModel by viewModels()
    companion object {
        const val TAG: String = "NameYourStoreDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inputField = EditText(context)

        val frameLayout = FrameLayout(requireContext()).apply {
            val verticalPadding = resources.getDimensionPixelSize(R.dimen.major_150)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.major_100)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            addView(inputField)
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.store_onboarding_name_your_store_dialog_title))
            .setView(frameLayout)
            .setPositiveButton(resources.getString(R.string.dialog_ok)) { dialog, _ ->
                viewModel.saveSiteTitle(inputField.text.toString())
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
