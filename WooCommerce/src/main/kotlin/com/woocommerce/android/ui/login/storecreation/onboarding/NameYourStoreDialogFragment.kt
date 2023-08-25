package com.woocommerce.android.ui.login.storecreation.onboarding

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NameYourStoreDialogFragment : DialogFragment() {
    @Inject
    internal lateinit var selectedSite: SelectedSite

    private val viewModel: StoreOnboardingViewModel by viewModels()

    private lateinit var dialog: AlertDialog
    private lateinit var layout: FrameLayout
    private lateinit var inputField: EditText
    private lateinit var progressBar: ProgressBar


    companion object {
        const val TAG: String = "NameYourStoreDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        inputField = EditText(context)
        progressBar = ProgressBar(context)

        progressBar.visibility = ProgressBar.GONE

        layout = FrameLayout(requireContext()).apply {
            val verticalPadding = resources.getDimensionPixelSize(R.dimen.major_150)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.major_100)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            addView(inputField)
            inputField.setText(selectedSite.get().name)
            addView(progressBar)
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.store_onboarding_name_your_store_dialog_title))
            .setView(layout)
            .setPositiveButton(resources.getString(R.string.dialog_ok), null)
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }

        dialog = builder.create()

        dialog.setOnShowListener {
            // Setting here to prevent automatic dismissal
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                viewModel.saveSiteTitle(inputField.text.toString(), fromOnboarding = false)
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isSavingSiteTitle.observe(viewLifecycleOwner) { isSavingSiteTitle ->
            when (isSavingSiteTitle) {
                StoreOnboardingViewModel.NameYourStoreDialogState.START,
                StoreOnboardingViewModel.NameYourStoreDialogState.FAILURE,
                null -> {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    inputField.visibility = EditText.VISIBLE
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    dialog.setCancelable(true)
                }

                StoreOnboardingViewModel.NameYourStoreDialogState.LOADING -> {
                    progressBar.visibility = ProgressBar.VISIBLE
                    inputField.visibility = EditText.INVISIBLE
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                    dialog.setCancelable(false)
                }

                StoreOnboardingViewModel.NameYourStoreDialogState.SUCCESS -> {
                    dialog.dismiss()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
