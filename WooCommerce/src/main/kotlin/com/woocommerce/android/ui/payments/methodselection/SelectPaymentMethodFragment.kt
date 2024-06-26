package com.woocommerce.android.ui.payments.methodselection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSelectPaymentMethodBinding
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectDialogFragment
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentDialogFragment
import com.woocommerce.android.ui.payments.changeduecalculator.ChangeDueCalculatorFragment
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewState.Loading
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewState.Success
import com.woocommerce.android.ui.payments.scantopay.ScanToPayDialogFragment
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryFragment
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderPaymentResult
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

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectPaymentMethodBinding.inflate(inflater, container, false)
        return if (viewModel.displayUi) {
            setupToolbar()
            binding.root
        } else {
            View(requireContext())
        }
    }

    private fun setupToolbar() {
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.ic_back_24dp
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpObservers(binding)
        setupResultHandlers()
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
            }
        }
    }

    private fun renderLoadingState(binding: FragmentSelectPaymentMethodBinding) {
        binding.container.isVisible = false
        binding.tvSelectPaymentTitle.isVisible = false
        binding.pbLoading.isVisible = true
        binding.learnMoreIppPaymentMethodsTv.learnMore.isVisible = false
    }

    private fun renderSuccessfulState(
        binding: FragmentSelectPaymentMethodBinding,
        state: Success
    ) {
        binding.container.isVisible = true
        binding.tvSelectPaymentTitle.isVisible = true
        binding.pbLoading.isVisible = false

        binding.toolbar.title = getString(R.string.simple_payments_take_payment_button, state.orderTotal)

        binding.container.removeAllViews()
        state.rows.forEach { row ->
            binding.container.addView(
                when (row) {
                    is Success.Row.Single -> row.buildSingleRowView()
                    is Success.Row.Double -> row.buildDoubleRow()
                }
            )

            binding.container.addView(MaterialDivider(requireContext()))
        }

        with(binding.learnMoreIppPaymentMethodsTv) {
            learnMore.setOnClickListener { state.learnMoreIpp.onClick.invoke() }
            UiHelpers.setTextOrHide(binding.learnMoreIppPaymentMethodsTv.learnMore, state.learnMoreIpp.label)
        }
    }

    private fun Success.Row.Single.buildSingleRowView() =
        layoutInflater.inflate(R.layout.item_select_payment_method_single_row, null)
            .apply {
                with(findViewById<TextView>(R.id.tvSelectPaymentRowHeader)) {
                    text = getString(label)
                    setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
                }

                with(findViewById<View>(R.id.vSelectPaymentRowOverlay)) {
                    isVisible = !this@buildSingleRowView.isEnabled
                }

                if (this@buildSingleRowView.isEnabled) setOnClickListener { onClick() }
            }

    private fun Success.Row.Double.buildDoubleRow() =
        layoutInflater.inflate(R.layout.item_select_payment_method_double_row, null)
            .apply {
                with(findViewById<TextView>(R.id.tvSelectPaymentRowHeader)) {
                    text = getString(label)
                }

                with(findViewById<TextView>(R.id.tvSelectPaymentRowDescription)) {
                    text = getString(description)
                }

                with(findViewById<ImageView>(R.id.ivSelectPaymentRowIcon)) {
                    setImageResource(icon)
                }

                with(findViewById<View>(R.id.vSelectPaymentRowOverlay)) {
                    isVisible = !this@buildDoubleRow.isEnabled
                }

                if (this@buildDoubleRow.isEnabled) setOnClickListener { onClick() }
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

                is SharePaymentUrlViaQr -> {
                    val action =
                        SelectPaymentMethodFragmentDirections
                            .actionSelectPaymentMethodFragmentToScanToPayDialogFragment(
                                event.paymentUrl
                            )
                    findNavController().navigate(action)
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
                    val action = if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToOrderDetailFragment(
                            orderId = event.order.id
                        )
                    } else {
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToOrderList()
                    }
                    findNavController().navigateSafely(action)
                }

                is NavigateBackToHub -> {
                    val action =
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToCardReaderHubFragment(
                            event.cardReaderFlowParam
                        )
                    findNavController().navigateSafely(action)
                }

                is OpenGenericWebView -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }

                is NavigateToOrderDetails -> {
                    val action =
                        SelectPaymentMethodFragmentDirections.actionSelectPaymentMethodFragmentToOrderDetailFragment(
                            orderId = event.orderId
                        )
                    findNavController().navigateSafely(action)
                }

                is NavigateToChangeDueCalculatorScreen -> {
                    val action =
                        SelectPaymentMethodFragmentDirections
                            .actionSelectPaymentMethodFragmentToChangeDueCalculatorFragment(
                                orderId = event.order.id
                            )
                    findNavController().navigate(action)
                }

                is NavigateToTapToPaySummary -> {
                    findNavController().navigateSafely(
                        SelectPaymentMethodFragmentDirections
                            .actionSelectPaymentMethodFragmentToTapToPaySummaryFragment(
                                TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(
                                    order = event.order
                                )
                            )
                    )
                }

                is ReturnResultToWooPos -> {
                    parentFragmentManager.setFragmentResult(
                        WooPosCardReaderActivity.WOO_POS_CARD_PAYMENT_REQUEST_KEY,
                        Bundle().apply {
                            putParcelable(
                                WooPosCardReaderActivity.WOO_POS_CARD_PAYMENT_RESULT_KEY,
                                event.asWooPosCardReaderPaymentResult(),
                            )
                        }
                    )
                }
            }
        }
    }

    private fun ReturnResultToWooPos.asWooPosCardReaderPaymentResult() =
        when (this) {
            is ReturnResultToWooPos.Success -> WooPosCardReaderPaymentResult.Success
            else -> WooPosCardReaderPaymentResult.Failure
        }

    private fun setupResultHandlers() {
        handleDialogResult<Boolean>(
            key = CardReaderConnectDialogFragment.KEY_CONNECT_TO_READER_RESULT,
            entryId = R.id.selectPaymentMethodFragment
        ) { connected ->
            viewModel.onConnectToReaderResultReceived(connected)
        }

        handleDialogNotice(
            key = CardReaderPaymentDialogFragment.KEY_CARD_PAYMENT_RESULT,
            entryId = R.id.selectPaymentMethodFragment
        ) {
            viewModel.onCardReaderPaymentCompleted()
        }

        handleDialogNotice(
            key = ScanToPayDialogFragment.KEY_SCAN_TO_PAY_RESULT,
            entryId = R.id.selectPaymentMethodFragment
        ) {
            viewModel.onScanToPayCompleted()
        }

        handleDialogResult<Boolean>(
            key = ChangeDueCalculatorFragment.IS_ORDER_PAID_RESULT,
            entryId = R.id.selectPaymentMethodFragment
        ) { paid ->
            viewModel.handleIsOrderPaid(paid)
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
