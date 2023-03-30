package com.woocommerce.android.ui.payments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSelectPaymentMethodBinding
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.DismissCardReaderUpsellBanner
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.DismissCardReaderUpsellBannerViaDontShowAgain
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.DismissCardReaderUpsellBannerViaRemindMeLater
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateBackToHub
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateBackToOrderList
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToCardReaderHubFlow
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToCardReaderPaymentFlow
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToCardReaderRefundFlow
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.NavigateToOrderDetails
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.OpenGenericWebView
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.OpenPurchaseCardReaderLink
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.SharePaymentUrl
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.ViewState.Loading
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.ViewState.Success
import com.woocommerce.android.ui.payments.banner.Banner
import com.woocommerce.android.ui.payments.banner.BannerState
import com.woocommerce.android.ui.payments.banner.PaymentsScreenBannerDismissDialog
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectDialogFragment
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentDialogFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectPaymentMethodFragment : BaseFragment(R.layout.fragment_select_payment_method), BackPressListener {
    private val viewModel: SelectPaymentMethodViewModel by viewModels()
    private val sharePaymentUrlLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onSharePaymentUrlCompleted()
    }

    private var _binding: FragmentSelectPaymentMethodBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectPaymentMethodBinding.inflate(inflater, container, false)

        val view = binding.root
        if (viewModel.shouldShowUpsellCardReaderDismissDialog.value == true) {
            applyBannerDismissDialogComposeUI()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpObservers(binding)
        setupResultHandlers()
    }

    private fun applyBannerComposeUI(state: BannerState) {
        binding.upsellCardReaderComposeView.upsellCardReaderBannerView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Banner(bannerState = state)
                }
            }
        }
    }

    private fun applyBannerDismissDialogComposeUI() {
        binding.upsellCardReaderComposeView.upsellCardReaderDismissView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    PaymentsScreenBannerDismissDialog(viewModel)
                }
            }
        }
    }

    private fun setUpObservers(binding: FragmentSelectPaymentMethodBinding) {
        handleViewState(binding)
        handleEvents(binding)
    }

    private fun handleViewState(binding: FragmentSelectPaymentMethodBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { state ->
            when (state) {
                Loading -> renderLoadingState(binding)
                is Success -> renderSuccessfulState(binding, state)
            }.exhaustive
        }
    }

    private fun renderLoadingState(binding: FragmentSelectPaymentMethodBinding) {
        binding.container.isVisible = false
        binding.pbLoading.isVisible = true
    }

    private fun renderSuccessfulState(
        binding: FragmentSelectPaymentMethodBinding,
        state: Success
    ) {
        binding.container.isVisible = true
        binding.pbLoading.isVisible = false
        requireActivity().title = getString(
            R.string.simple_payments_take_payment_button,
            state.orderTotal
        )

        binding.textCash.setOnClickListener {
            viewModel.onCashPaymentClicked()
        }

        with(binding.tvTapToPay) {
            isVisible = state.isPaymentCollectableWithTapToPay
            setOnClickListener {
                viewModel.onTapToPayClicked()
            }
        }
        binding.divider3.isVisible = state.isPaymentCollectableWithTapToPay

        with(binding.tvBtReader) {
            isVisible = state.isPaymentCollectableWithExternalCardReader
            setOnClickListener {
                viewModel.onBtReaderClicked()
            }
        }
        binding.divider4.isVisible = state.isPaymentCollectableWithExternalCardReader

        with(binding.textShare) {
            isVisible = state.paymentUrl.isNotEmpty()
            setOnClickListener {
                viewModel.onSharePaymentUrlClicked()
            }
        }

        with(binding.learnMoreIppPaymentMethodsTv) {
            learnMore.setOnClickListener { state.learMoreIpp.onClick.invoke() }
            UiHelpers.setTextOrHide(binding.learnMoreIppPaymentMethodsTv.learnMore, state.learMoreIpp.label)
        }

        applyBannerComposeUI(state.bannerState)
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun handleEvents(binding: FragmentSelectPaymentMethodBinding) {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is ShowDialog -> {
                    event.showDialog()
                }
                is ShowSnackbar -> {
                    Snackbar.make(
                        binding.container,
                        event.message,
                        BaseTransientBottomBar.LENGTH_LONG
                    ).show()
                }
                is SharePaymentUrl -> {
                    sharePaymentUrl(event.storeName, event.paymentUrl)
                }
                is NavigateToCardReaderPaymentFlow -> {
                    val action =
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToCardReaderPaymentFlow(
                            event.cardReaderFlowParam,
                            event.cardReaderType
                        )
                    findNavController().navigate(action)
                }
                is NavigateToCardReaderHubFlow -> {
                    val action =
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToCardReaderHubFlow(
                            event.cardReaderFlowParam
                        )
                    findNavController().navigate(action)
                }
                is NavigateToCardReaderRefundFlow -> {
                    val action =
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToCardReaderRefundFlow(
                            event.cardReaderFlowParam,
                            event.cardReaderType
                        )
                    findNavController().navigate(action)
                }
                is NavigateBackToOrderList -> {
                    val action = SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToOrderList()
                    findNavController().navigateSafely(action)
                }
                is NavigateBackToHub -> {
                    val action = SelectPaymentMethodFragmentDirections
                        .actionSelectPaymentMethodFragmentToCardReaderHubFragment(
                            event.cardReaderFlowParam
                        )
                    findNavController().navigateSafely(action)
                }
                DismissCardReaderUpsellBanner -> {
                    applyBannerDismissDialogComposeUI()
                }
                DismissCardReaderUpsellBannerViaRemindMeLater -> {
                    binding.upsellCardReaderComposeView.upsellCardReaderBannerView.visibility = View.GONE
                    binding.upsellCardReaderComposeView.upsellCardReaderDismissView.visibility = View.GONE
                }
                DismissCardReaderUpsellBannerViaDontShowAgain -> {
                    binding.upsellCardReaderComposeView.upsellCardReaderBannerView.visibility = View.GONE
                    binding.upsellCardReaderComposeView.upsellCardReaderDismissView.visibility = View.GONE
                }
                is OpenPurchaseCardReaderLink -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            title = resources.getString(event.titleRes)
                        )
                    )
                }
                is OpenGenericWebView -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is NavigateToOrderDetails -> {
                    val action = SelectPaymentMethodFragmentDirections
                        .actionSelectPaymentMethodFragmentToOrderDetailFragment(
                            orderId = event.orderId
                        )
                    findNavController().navigateSafely(action)
                }
            }
        }
    }

    private fun setupResultHandlers() {
        handleDialogResult<Boolean>(
            key = CardReaderConnectDialogFragment.KEY_CONNECT_TO_READER_RESULT,
            entryId = R.id.selectPaymentMethodFragment
        ) { connected ->
            viewModel.onConnectToReaderResultReceived(connected)
        }

        handleDialogNotice<String>(
            key = CardReaderPaymentDialogFragment.KEY_CARD_PAYMENT_RESULT,
            entryId = R.id.selectPaymentMethodFragment
        ) {
            viewModel.onCardReaderPaymentCompleted()
        }
    }

    private fun sharePaymentUrl(storeName: String, paymentUrl: String) {
        val title = getString(R.string.simple_payments_share_payment_dialog_title, storeName)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, paymentUrl)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = "text/plain"
        }
        sharePaymentUrlLauncher.launch(Intent.createChooser(shareIntent, title))
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackPressed()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
