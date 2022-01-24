package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentTakePaymentBinding
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentDialogFragment
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectDialogFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TakePaymentFragment : BaseFragment(R.layout.fragment_take_payment) {
    private val viewModel: TakePaymentViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<SimplePaymentsSharedViewModel>(R.id.nav_graph_main)

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTakePaymentBinding.bind(view)
        binding.textCash.setOnClickListener {
            viewModel.onCashPaymentClicked()
        }
        binding.textCard.setOnClickListener {
            viewModel.onCardPaymentClicked()
        }

        setUpObservers()
        setupResultHandlers()
    }

    private fun setUpObservers() {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is MultiLiveEvent.Event.ShowDialog -> {
                        event.showDialog()
                    }
                    is MultiLiveEvent.Event.ShowSnackbar -> {
                        uiMessageResolver.showSnack(event.message)
                    }
                    is MultiLiveEvent.Event.Exit -> {
                        findNavController().navigateSafely(R.id.orders)
                    }
                    is OrderNavigationTarget.StartCardReaderConnectFlow -> {
                        val action = TakePaymentFragmentDirections.actionTakePaymentFragmentToCardReaderConnectDialog(
                            skipOnboarding = event.skipOnboarding
                        )
                        findNavController().navigateSafely(action)
                    }
                    is OrderNavigationTarget.StartCardReaderPaymentFlow -> {
                        val action = TakePaymentFragmentDirections.actionTakePaymentFragmentToCardReaderPaymentDialog(
                            orderId = event.orderId
                        )
                        findNavController().navigateSafely(action)
                    }
                }
            }
        )
    }

    private fun setupResultHandlers() {
        handleResult<Boolean>(CardReaderConnectDialogFragment.KEY_CONNECT_TO_READER_RESULT) { connected ->
            viewModel.onConnectToReaderResultReceived(connected)
        }

        handleDialogNotice<String>(
            key = CardReaderPaymentDialogFragment.KEY_CARD_PAYMENT_RESULT,
            entryId = R.id.takePaymentFragment
        ) {
            viewModel.onCardReaderPaymentCompleted()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
    }

    override fun getFragmentTitle(): String {
        val totalStr = sharedViewModel.formatAmount(viewModel.orderTotal)
        return getString(R.string.simple_payments_take_payment_button, totalStr)
    }
}
