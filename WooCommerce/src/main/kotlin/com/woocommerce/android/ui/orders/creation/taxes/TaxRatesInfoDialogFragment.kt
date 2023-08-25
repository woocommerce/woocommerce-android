package com.woocommerce.android.ui.orders.creation.taxes

import android.app.Dialog
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils

class TaxRatesInfoDialogFragment : DialogFragment() {
    private val args: TaxRatesInfoDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext()).setView(
            ComposeView(requireContext()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WooThemeWithBackground {
                        TaxRateInfoDialog(args.dialogState, ::dismiss, ::goToTaxRatesSettings)
                    }
                }
            }
        )
            .setCancelable(true)
            .create()
    }

    private fun goToTaxRatesSettings() {
        args.dialogState.taxRatesSettingsUrl.let {
            ChromeCustomTabUtils.launchUrl(requireContext(), it)
            dismiss()
        }
    }
}
