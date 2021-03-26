package com.woocommerce.android.ui.prefs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerceDebug
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsCardReaderBinding

class CardReaderSettingsFragment : Fragment(R.layout.fragment_settings_card_reader) {
    companion object {
        const val TAG = "card-reader-settings"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // TODO cardreader move this into a VM
                connectToReader()
            } else {
                // TODO cardreader Move this into a VM and use string resource
                Snackbar.make(requireView(), "Missing required permissions", BaseTransientBottomBar.LENGTH_SHORT)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsCardReaderBinding.bind(view)
        binding.connectReaderButton.setOnClickListener {
            // TODO cardreader move this into a vm
            // TODO card reader implement connect reader button
            val permissionType = Manifest.permission.ACCESS_COARSE_LOCATION
            val locationPermissionResult = ContextCompat.checkSelfPermission(
                requireContext(),
                permissionType
            )

            if (locationPermissionResult == PackageManager.PERMISSION_GRANTED) {
                connectToReader()
            } else {
                requestPermissionLauncher.launch(permissionType)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings_card_reader)
    }

    private fun connectToReader() {
        // TODO cardreader move this into a VM
        // TODO cardreader Replace WooCommerceDebug with WooCommerce to support production builds
        (requireActivity().application as? WooCommerceDebug)?.let {
            if (!it.cardReaderManager.isInitialized()) {
                it.cardReaderManager.initialize(it)
            }
        }
    }
}
