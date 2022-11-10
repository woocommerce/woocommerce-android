package com.woocommerce.android.ui.sitepicker

import android.os.Bundle
import android.view.View
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogSitePickerAddStoreBottomSheetBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class AddStoreBottomSheetFragment : WCBottomSheetDialogFragment(R.layout.dialog_site_picker_add_store_bottom_sheet) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogSitePickerAddStoreBottomSheetBinding.bind(view)

        binding.createNewStoreButton.setOnClickListener {
            AnalyticsTracker.track(
                AnalyticsEvent.SITE_PICKER_CREATE_SITE_TAPPED,
                mapOf(AnalyticsTracker.KEY_SOURCE to AppPrefs.getStoreCreationSource())
            )
            val directions = when {
                FeatureFlag.NATIVE_STORE_CREATION_FLOW.isEnabled() ->
                    AddStoreBottomSheetFragmentDirections.actionAddStoreBottomSheetFragmentToStoreCreationNativeFlow()
                else ->
                    AddStoreBottomSheetFragmentDirections
                        .actionAddStoreBottomSheetFragmentToWebViewStoreCreationFragment()
            }
            findNavController().navigateSafely(
                directions = directions,
                navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.sitePickerFragment, false)
                    .build()
            )
        }
        binding.connectExistingStoreButton.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.SITE_PICKER_CONNECT_EXISTING_STORE_TAPPED)

            findNavController().navigateSafely(
                directions = AddStoreBottomSheetFragmentDirections
                    .actionAddSiteBottomSheetFragmentToSitePickerSiteDiscoveryFragment(),
                navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.sitePickerFragment, false)
                    .build()
            )
        }
    }
}
