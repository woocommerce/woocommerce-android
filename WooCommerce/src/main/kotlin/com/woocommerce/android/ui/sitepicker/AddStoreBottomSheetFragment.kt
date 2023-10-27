package com.woocommerce.android.ui.sitepicker

import android.os.Bundle
import android.view.View
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogSitePickerAddStoreBottomSheetBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddStoreBottomSheetFragment : WCBottomSheetDialogFragment(R.layout.dialog_site_picker_add_store_bottom_sheet) {
    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogSitePickerAddStoreBottomSheetBinding.bind(view)

        binding.createNewStoreButton.setOnClickListener {
            AnalyticsTracker.track(
                AnalyticsEvent.SITE_PICKER_CREATE_SITE_TAPPED,
                mapOf(AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource())
            )
            AnalyticsTracker.track(
                AnalyticsEvent.SITE_CREATION_FLOW_STARTED,
                mapOf(AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource())
            )
            findNavController().navigateSafely(
                directions = AddStoreBottomSheetFragmentDirections
                    .actionAddStoreBottomSheetFragmentToStoreCreationNativeFlow(),
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
