package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentTakePaymentBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
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

        setUpObservers()
        val binding = FragmentTakePaymentBinding.bind(view)

        binding.textCash.setOnClickListener {
            viewModel.onCashPaymentClicked()
        }
        binding.textCard.setOnClickListener {
            viewModel.onCardPaymentClicked()
        }
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
                        switchToCardReaderGraph(R.id.simplePayment_cardReaderConnectDialogFragment)
                    }
                    is OrderNavigationTarget.StartCardReaderPaymentFlow -> {
                        switchToCardReaderGraph(R.id.simplePayment_cardReaderPaymentDialog)
                    }
                }
            }
        )
    }

    private fun switchToCardReaderGraph(@IdRes startDestinationId: Int, directions: NavDirections? = null) {
        val navController = findNavController()
        if (findNavController().graph.id == R.navigation.nav_graph_simple_payments_card_payment) {
            navController.navigateSafely(startDestinationId, directions)
        } else {
            val graph = navController.navInflater.inflate(R.navigation.nav_graph_simple_payments_card_payment)
            graph.setStartDestination(startDestinationId)
            navController.setGraph(graph, directions?.arguments)
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
