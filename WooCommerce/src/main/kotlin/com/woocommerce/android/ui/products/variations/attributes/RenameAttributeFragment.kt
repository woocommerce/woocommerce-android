package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRenameAttributeBinding

class RenameAttributeFragment : Fragment(R.layout.fragment_rename_attribute) {
    companion object {
        const val TAG: String = "RenameAttributeFragment"
    }

    private var _binding: FragmentRenameAttributeBinding? = null
    private val binding get() = _binding!!

    private val navArgs: RenameAttributeFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRenameAttributeBinding.bind(view)
        requireActivity().title = requireActivity().getString(R.string.product_rename_attribute)

        if (savedInstanceState == null) {
            binding.attributeEditText.setText(navArgs.attributeName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
