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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Failed
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Started
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Succeeded
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus.CONNECTED
import com.woocommerce.android.cardreader.CardReaderStatus.CONNECTING
import com.woocommerce.android.cardreader.CardReaderStatus.NOT_CONNECTED
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Installing
import com.woocommerce.android.databinding.FragmentSettingsCardReaderBinding
import com.woocommerce.android.extensions.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

// TODO cardreader update this comment
/**
 * This fragment currently contains a UI for testing purposes. It'll be removed before the release.
 */
@AndroidEntryPoint
class CardReaderSettingsFragment : Fragment(R.layout.fragment_settings_card_reader), CoroutineScope {
    companion object {
        const val TAG = "card-reader-settings"
    }

    @Inject lateinit var cardReaderManager: CardReaderManager

    protected var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

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
        startObserving(binding)
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
        }
        binding.updateReaderSoftware.setOnClickListener {
            launch(Dispatchers.Default) {
                try {
                    cardReaderManager.let {
                        updateReaderSoftware(it, binding.softwareUpdateStatus)
                    }
                } catch (e: CancellationException) {
                }
            }
        }
        binding.redirectToDetailFragment.setOnClickListener {
            findNavController().navigateSafely(R.id.action_cardReaderSettingsFragment_to_cardReaderDetailFragment)
        }
    }

    private fun startObserving(binding: FragmentSettingsCardReaderBinding) {
        (requireActivity().application as? WooCommerce)?.let { application ->
            // TODO cardreader Move this into a VM
            lifecycleScope.launchWhenResumed {
                cardReaderManager.readerStatus.collect { status ->
                    binding.connectionStatus.text = status.name
                    when (status) {
                        CONNECTING, NOT_CONNECTED -> binding.updateReaderSoftware.isEnabled = false
                        CONNECTED -> binding.updateReaderSoftware.isEnabled = true
                    }
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
        cardReaderManager.let { cardReaderManager ->
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
                                val success = cardReaderManager.connectToReader(event.list[0]) ?: false
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

    private suspend fun updateReaderSoftware(
        cardReaderManager: CardReaderManager,
        softwareUpdateStatus: MaterialTextView
    ) {
        cardReaderManager.updateSoftware().collect { event ->
            withContext(Dispatchers.Main) {
                softwareUpdateStatus.setText(
                    event.javaClass.simpleName + if (event is Installing) {
                        " ${event.progress * 100}%"
                    } else {
                        ""
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
