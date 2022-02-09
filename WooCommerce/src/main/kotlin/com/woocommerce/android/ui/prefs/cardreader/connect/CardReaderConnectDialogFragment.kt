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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.CardReaderConnectDialogBinding
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToParentWithResult
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckBluetoothEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckBluetoothPermissionsGiven
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckLocationEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.OpenLocationSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.RequestBluetoothRuntimePermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.RequestEnableBluetooth
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectEvent.ShowCardReaderTutorial
import com.woocommerce.android.ui.prefs.cardreader.connect.adapter.MultipleCardReadersFoundAdapter
import com.woocommerce.android.ui.prefs.cardreader.tutorial.CardReaderTutorialDialogFragment
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateDialogFragment
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.LocationUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderConnectDialogFragment : DialogFragment(R.layout.card_reader_connect_dialog) {
    val viewModel: CardReaderConnectViewModel by viewModels()

    @Inject lateinit var locationUtils: LocationUtils

    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        (viewModel.event.value as? RequestLocationPermissions)?.onPermissionsRequestResult?.invoke(isGranted)
    }

    private val requestBluetoothPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            (viewModel.event.value as? RequestBluetoothRuntimePermissions)
                ?.onBluetoothRuntimePermissionsRequestResult
                ?.invoke(permissions.all { it.value })
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

        val binding = CardReaderConnectDialogBinding.bind(view)
        initMultipleReadersFoundRecyclerView(binding)
        initObservers(binding)
    }

    private fun initMultipleReadersFoundRecyclerView(binding: CardReaderConnectDialogBinding) {
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

    private fun initObservers(binding: CardReaderConnectDialogBinding) {
        observeEvents()
        observeState(binding)
        setupResultHandlers(viewModel)
    }

    private fun observeState(binding: CardReaderConnectDialogBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { viewState ->
            if (viewState is CardReaderConnectViewState.ReaderFoundState) {
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
        handleResult<UpdateResult>(CardReaderUpdateDialogFragment.KEY_READER_UPDATE_RESULT) {
            viewModel.onUpdateReaderResult(it)
        }
    }

    /**
     * When a reader is found, we fade out the scanning illustration, update the UI to the new state, then
     * fade in the reader found illustration
     */
    private fun moveToReaderFoundState(binding: CardReaderConnectDialogBinding, viewState: CardReaderConnectViewState) {
        val fadeOut = WooAnimUtils.getFadeOutAnim(binding.illustration, WooAnimUtils.Duration.LONG)
        val fadeIn = WooAnimUtils.getFadeInAnim(binding.illustration, WooAnimUtils.Duration.LONG)

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // make sure we haven't moved to another state before starting the fade in animation
                if (viewModel.viewStateData.value is CardReaderConnectViewState.ReaderFoundState) {
                    moveToState(binding, viewState)
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) fadeIn.start()
                }
            }
        })
        fadeOut.start()
    }

    private fun moveToState(binding: CardReaderConnectDialogBinding, viewState: CardReaderConnectViewState) {
        UiHelpers.setTextOrHide(binding.headerLabel, viewState.headerLabel)
        UiHelpers.setImageOrHideInLandscape(binding.illustration, viewState.illustration)
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

    @Suppress("ComplexMethod", "LongMethod")
    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CheckLocationPermissions -> {
                    event.onLocationPermissionsCheckResult(
                        WooPermissionUtils.hasFineLocationPermission(requireContext())
                    )
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
                is CheckBluetoothPermissionsGiven -> {
                    event.onBluetoothPermissionsGivenCheckResult(
                        WooPermissionUtils.hasBluetoothScanPermission(requireContext()) &&
                            WooPermissionUtils.hasBluetoothConnectPermission(requireContext())
                    )
                }
                is RequestBluetoothRuntimePermissions -> {
                    WooPermissionUtils.requestScanAndConnectBluetoothPermission(requestBluetoothPermissionsLauncher)
                }
                is CheckBluetoothEnabled -> {
                    val btAdapter = BluetoothAdapter.getDefaultAdapter()
                    event.onBluetoothCheckResult(btAdapter?.isEnabled ?: false)
                }
                is RequestEnableBluetooth -> {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestEnableBluetoothLauncher.launch(enableBtIntent)
                }
                is ShowCardReaderTutorial -> {
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderConnectDialogFragment_to_cardReaderTutorialDialogFragment)
                }
                is CardReaderConnectEvent.ShowUpdateInProgress -> {
                    findNavController().navigateSafely(
                        CardReaderConnectDialogFragmentDirections
                            .actionCardReaderConnectDialogFragmentToCardReaderUpdateDialogFragment(
                                requiredUpdate = true
                            )
                    )
                }
                is CardReaderConnectEvent.NavigateToOnboardingFlow -> {
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderConnectDialogFragment_to_cardReaderOnboardingFragment)
                }
                is ExitWithResult<*> -> {
                    navigateToParentWithResult(
                        key = KEY_CONNECT_TO_READER_RESULT,
                        result = event.data as Boolean,
                        childId = R.id.cardReaderConnectDialogFragment,
                    )
                }
                is CardReaderConnectEvent.ShowToast ->
                    ToastUtils.showToast(requireContext(), getString(event.message))
                is CardReaderConnectEvent.OpenWPComWebView ->
                    findNavController().navigateSafely(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(urlToLoad = event.url)
                    )
                is CardReaderConnectEvent.OpenGenericWebView ->
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                else -> event.isHandled = false
            }
        }
    }

    private fun updateMultipleReadersFoundRecyclerView(
        binding: CardReaderConnectDialogBinding,
        viewState: CardReaderConnectViewState
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
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStarted()
    }

    companion object {
        const val KEY_CONNECT_TO_READER_RESULT = "key_connect_to_reader_result"
    }
}
