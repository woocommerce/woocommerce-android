package com.woocommerce.android.ui.prefs.cardreader.connect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.databinding.FragmentCardReaderConnectBinding
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckBluetoothEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenLocationSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestEnableBluetooth
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.ShowCardReaderTutorial
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState
import com.woocommerce.android.ui.prefs.cardreader.connect.adapter.MultipleCardReadersFoundAdapter
import com.woocommerce.android.ui.prefs.cardreader.tutorial.CardReaderTutorialDialogFragment
import com.woocommerce.android.util.LocationUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderConnectFragment : DialogFragment(R.layout.fragment_card_reader_connect) {
    val viewModel: CardReaderConnectViewModel by viewModels()

    @Inject lateinit var locationUtils: LocationUtils
    @Inject lateinit var cardReaderManager: CardReaderManager

    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        (viewModel.event.value as? RequestLocationPermissions)?.onPermissionsRequestResult?.invoke(isGranted)
    }

    private val requestEnableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) { activityResult ->
        (viewModel.event.value as? RequestEnableBluetooth)?.onEnableBluetoothRequestResult
            ?.invoke(activityResult.resultCode == RESULT_OK)
    }

    private val requestEnableLocationProviderLauncher = registerForActivityResult(StartActivityForResult()) {
        (viewModel.event.value as? OpenLocationSettings)?.onLocationSettingsClosed?.invoke()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.let {
            it.setCanceledOnTouchOutside(false)
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderConnectBinding.bind(view)
        initMultipleReadersFoundRecyclerView(binding)
        initObservers(binding)
    }

    private fun initMultipleReadersFoundRecyclerView(binding: FragmentCardReaderConnectBinding) {
        binding.multipleCardReadersFoundRv.layoutManager = LinearLayoutManager(requireContext())
        binding.multipleCardReadersFoundRv.addItemDecoration(
            AlignedDividerDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL,
                R.id.readers_found_reader_id
            )
        )
        binding.multipleCardReadersFoundRv.adapter = MultipleCardReadersFoundAdapter()
    }

    private fun initObservers(binding: FragmentCardReaderConnectBinding) {
        observeEvents()
        observeState(binding)
        setupResultHandlers(viewModel)
    }

    private fun observeState(binding: FragmentCardReaderConnectBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { viewState ->
            if (viewState is ViewState.ReaderFoundState) {
                moveToReaderFoundState(binding, viewState)
            } else {
                moveToState(binding, viewState)
            }
        }
    }

    private fun setupResultHandlers(viewModel: CardReaderConnectViewModel) {
        handleNotice(CardReaderTutorialDialogFragment.KEY_READER_TUTORIAL_RESULT) {
            viewModel.onTutorialClosed()
        }
    }

    /**
     * When a reader is found, we fade out the scanning illustration, update the UI to the new state, then
     * fade in the reader found illustration
     */
    private fun moveToReaderFoundState(binding: FragmentCardReaderConnectBinding, viewState: ViewState) {
        val fadeOut = WooAnimUtils.getFadeOutAnim(binding.illustration, WooAnimUtils.Duration.LONG)
        val fadeIn = WooAnimUtils.getFadeInAnim(binding.illustration, WooAnimUtils.Duration.LONG)

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // make sure we haven't moved to another state before starting the fade in animation
                if (viewModel.viewStateData.value is ViewState.ReaderFoundState) {
                    moveToState(binding, viewState)
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) fadeIn.start()
                }
            }
        })
        fadeOut.start()
    }

    private fun moveToState(binding: FragmentCardReaderConnectBinding, viewState: ViewState) {
        UiHelpers.setTextOrHide(binding.headerLabel, viewState.headerLabel)
        UiHelpers.setImageOrHide(binding.illustration, viewState.illustration)
        UiHelpers.setTextOrHide(binding.hintLabel, viewState.hintLabel)
        UiHelpers.setTextOrHide(binding.primaryActionBtn, viewState.primaryActionLabel)
        UiHelpers.setTextOrHide(binding.secondaryActionBtn, viewState.secondaryActionLabel)
        binding.primaryActionBtn.setOnClickListener {
            viewState.onPrimaryActionClicked?.invoke()
        }
        binding.secondaryActionBtn.setOnClickListener {
            viewState.onSecondaryActionClicked?.invoke()
        }

        with(binding.illustration.layoutParams as ViewGroup.MarginLayoutParams) {
            topMargin = resources.getDimensionPixelSize(viewState.illustrationTopMargin)
        }

        updateMultipleReadersFoundRecyclerView(binding, viewState)

        // the scanning for readers and connecting to reader images are AnimatedVectorDrawables
        (binding.illustration.drawable as? AnimatedVectorDrawable)?.start()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CheckLocationPermissions -> {
                    event.onPermissionsCheckResult(WooPermissionUtils.hasFineLocationPermission(requireContext()))
                }
                is RequestLocationPermissions -> {
                    WooPermissionUtils.requestFineLocationPermission(requestPermissionLauncher)
                }
                is OpenPermissionsSettings -> {
                    WooPermissionUtils.showAppSettings(requireContext(), openInNewStack = false)
                }
                is CheckLocationEnabled -> {
                    event.onLocationEnabledCheckResult(locationUtils.isLocationEnabled())
                }
                is OpenLocationSettings -> {
                    openLocationSettings()
                }
                is CheckBluetoothEnabled -> {
                    val btAdapter = BluetoothAdapter.getDefaultAdapter()
                    event.onBluetoothCheckResult(btAdapter?.isEnabled ?: false)
                }
                is RequestEnableBluetooth -> {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestEnableBluetoothLauncher.launch(enableBtIntent)
                }
                is InitializeCardReaderManager -> {
                    cardReaderManager.let {
                        it.initialize(requireActivity().application)
                        event.onCardManagerInitialized(it)
                    }
                }
                is ShowCardReaderTutorial -> {
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderConnectFragment_to_cardReaderTutorialDialogFragment)
                }
                is ExitWithResult<*> -> {
                    navigateBackWithResult(KEY_CONNECT_TO_READER_RESULT, event.data as Boolean)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun updateMultipleReadersFoundRecyclerView(
        binding: FragmentCardReaderConnectBinding,
        viewState: ViewState
    ) {
        (binding.multipleCardReadersFoundRv.adapter as MultipleCardReadersFoundAdapter)
            .list = viewState.listItems ?: listOf()
        UiHelpers.updateVisibility(binding.multipleCardReadersFoundRv, viewState.listItems != null)
    }

    private fun openLocationSettings() {
        try {
            val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            requestEnableLocationProviderLauncher.launch(enableLocationIntent)
        } catch (e: ActivityNotFoundException) {
            WooLog.e(WooLog.T.CARD_READER, "Location settings activity not found.")
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        viewModel.onScreenResumed()
    }

    companion object {
        const val KEY_CONNECT_TO_READER_RESULT = "key_connect_to_reader_result"
    }
}
