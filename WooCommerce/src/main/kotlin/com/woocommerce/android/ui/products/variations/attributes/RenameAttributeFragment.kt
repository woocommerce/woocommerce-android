package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRenameAttributeBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import org.wordpress.android.util.ActivityUtils

class RenameAttributeFragment : Fragment(R.layout.fragment_rename_attribute), BackPressListener {
    companion object {
        const val TAG: String = "RenameAttributeFragment"
        const val KEY_RENAME_ATTRIBUTE_RESULT = "key_rename_attribute_result"
    }

    private var _binding: FragmentRenameAttributeBinding? = null
    private val binding get() = _binding!!

    private val navArgs: RenameAttributeFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRenameAttributeBinding.bind(view)
        requireActivity().title = getString(R.string.product_rename_attribute)

        if (savedInstanceState == null) {
            binding.attributeName.setText(navArgs.attributeName)
        }

        binding.attributeName.setOnEditorActionListener { termName: String ->
            if (termName.isNotBlank()) {
                navigateBack()
            }
            true
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

    override fun onStop() {
        super.onStop()
        ActivityUtils.hideKeyboard(requireActivity())
    }

    override fun onRequestAllowBackPress(): Boolean {
        navigateBack()
        return false
    }

    private fun navigateBack() {
        val attributeName = binding.attributeName.getText()
        if (attributeName.isNotEmpty() && !attributeName.equals(navArgs.attributeName)) {
            navigateBackWithResult(KEY_RENAME_ATTRIBUTE_RESULT, attributeName)
        } else {
            findNavController().navigateUp()
        }
    }
}
