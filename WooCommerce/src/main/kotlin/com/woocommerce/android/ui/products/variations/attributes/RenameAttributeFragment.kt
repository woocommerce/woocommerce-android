package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRenameAttributeBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import org.wordpress.android.util.ActivityUtils

class RenameAttributeFragment : BaseFragment(R.layout.fragment_rename_attribute), BackPressListener {
    companion object {
        const val TAG: String = "RenameAttributeFragment"
        const val KEY_RENAME_ATTRIBUTE_RESULT = "key_rename_attribute_result"
    }

    private var _binding: FragmentRenameAttributeBinding? = null
    private val binding get() = _binding!!

    private val navArgs: RenameAttributeFragmentArgs by navArgs()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRenameAttributeBinding.bind(view)

        if (savedInstanceState == null) {
            binding.attributeName.text = navArgs.attributeName
        }

        binding.attributeName.setOnEditorActionListener { attributeName: String ->
            if (attributeName.isNotBlank()) {
                navigateBack(attributeName)
            }
            true
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_rename_attribute),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    navigateBack(binding.attributeName.text)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        binding.attributeName.showKeyboard(selectAll = true)
    }

    override fun onStop() {
        super.onStop()
        ActivityUtils.hideKeyboard(requireActivity())
    }

    override fun onRequestAllowBackPress(): Boolean {
        val attributeName = binding.attributeName.text
        navigateBack(attributeName)
        return false
    }

    private fun navigateBack(attributeName: String) {
        if (attributeName.isNotEmpty() && !attributeName.equals(navArgs.attributeName)) {
            navigateBackWithResult(KEY_RENAME_ATTRIBUTE_RESULT, attributeName)
        } else {
            findNavController().navigateUp()
        }
    }
}
