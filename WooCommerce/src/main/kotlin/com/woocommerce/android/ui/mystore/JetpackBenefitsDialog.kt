package com.woocommerce.android.ui.mystore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogJetpackBenefitsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JetpackBenefitsDialog : DialogFragment(R.layout.dialog_jetpack_benefits) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        val binding = DialogJetpackBenefitsBinding.bind(view)
        binding.dismissButton.setOnClickListener {
            dismiss()
        }
        binding.installJetpackButton.setOnClickListener {
            // TODO navigate to the jetpack installation screen
        }
    }
}
