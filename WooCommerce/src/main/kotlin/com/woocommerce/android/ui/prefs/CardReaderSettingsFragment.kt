package com.woocommerce.android.ui.prefs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Failed
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Started
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Succeeded
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.databinding.FragmentSettingsCardReaderBinding
import kotlinx.coroutines.flow.collect
import org.wordpress.android.util.AppLog

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
                connectToReader(view?.findViewById<CheckBox>(R.id.simulated_checkbox)?.isChecked == true)
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
            // TODO cardreader implement connect reader button
            val permissionType = Manifest.permission.ACCESS_FINE_LOCATION
            // TODO cardreader Replace with WooPermissionsUtils
            val locationPermissionResult = ContextCompat.checkSelfPermission(
                requireContext(),
                permissionType
            )

            if (locationPermissionResult == PackageManager.PERMISSION_GRANTED) {
                connectToReader(binding.simulatedCheckbox.isChecked)
            } else {
                requestPermissionLauncher.launch(permissionType)
            }
            startObserving(binding)
        }
    }

    private fun startObserving(binding: FragmentSettingsCardReaderBinding) {
        (requireActivity().application as? WooCommerce)?.let { application ->
            // TODO cardreader Move this into a VM
            lifecycleScope.launchWhenResumed {
                application.cardReaderManager?.readerStatus?.collect { status ->
                    binding.connectionStatus.text = status.name
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings_card_reader)
    }

    // TODO cardreader move this into a VM
    private fun connectToReader(simulated: Boolean) {
        getCardReaderManager()?.let { cardReaderManager ->
            if (!cardReaderManager.isInitialized) {
                cardReaderManager.initialize(requireActivity().application)
            }
            lifecycleScope.launchWhenResumed {
                cardReaderManager.discoverReaders(isSimulated = simulated).collect { event ->
                    AppLog.d(AppLog.T.MAIN, event.toString())
                    when (event) {
                        Started -> {
                            Snackbar.make(
                                requireView(),
                                event.javaClass.simpleName,
                                BaseTransientBottomBar.LENGTH_SHORT
                            ).show()
                        }
                        Succeeded, is Failed -> {
                            // no-op
                        }
                        is ReadersFound -> {
                            if (event.list.isNotEmpty()) {
                                val success = getCardReaderManager()?.connectToReader(event.list[0]) ?: false
                                Snackbar.make(
                                    requireView(),
                                    "Connecting to reader ${if (success) "succeeded" else "failed"}",
                                    BaseTransientBottomBar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getCardReaderManager(): CardReaderManager? =
        (requireActivity().application as? WooCommerce)?.cardReaderManager
}
