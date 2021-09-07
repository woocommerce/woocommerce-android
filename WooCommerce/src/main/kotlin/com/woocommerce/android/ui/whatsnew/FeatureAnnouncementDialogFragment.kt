package com.woocommerce.android.ui.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementDialogFragmentBinding

class FeatureAnnouncementDialogFragment : DialogFragment() {
    companion object {
        const val TAG: String = "FeatureAnnouncementDialog"
    }

    override fun getTheme(): Int {
        return R.style.Theme_Woo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)
        val binding = FeatureAnnouncementDialogFragmentBinding.bind(view)

        binding.closeFeatureAnnouncementButton.setOnClickListener {
            findNavController().popBackStack()
        }

        return view
    }
}
