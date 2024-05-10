package com.woocommerce.android.ui.payments.taptopay.summary

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryViewModel.NavigateToOrderDetails
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryViewModel.ShowSuccessfulRefundNotification
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryViewModel.StartTryPaymentFlow
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class TapToPaySummaryFragment : BaseFragment() {
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: TapToPaySummaryViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    TapToPaySummaryScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowSuccessfulRefundNotification -> {
                    Snackbar.make(
                        requireView(),
                        getString(event.message),
                        BaseTransientBottomBar.LENGTH_LONG
                    ).setAction(getString(event.actionLabel)) { event.action.invoke() }
                        .show()
                }
                is StartTryPaymentFlow -> {
                    findNavController().navigate(
                        TapToPaySummaryFragmentDirections
                            .actionTapToPaySummaryFragmentToSimplePaymentsFragment(
                                event.order,
                                PaymentType.TRY_TAP_TO_PAY,
                            )
                    )
                }
                is NavigateToOrderDetails -> {
                    findNavController().navigate(
                        TapToPaySummaryFragmentDirections.actionTapToPaySummaryFragmentToOrderDetailFragment(
                            event.orderId
                        )
                    )
                }
                is TapToPaySummaryViewModel.NavigateTTPAboutScreen ->
                    findNavController().navigate(
                        TapToPaySummaryFragmentDirections.actionTapToPaySummaryFragmentToTapToPayAboutFragment(
                            event.cardReaderConfigForSupportedCountry
                        )
                    )
                else -> event.isHandled = false
            }
        }
    }

    sealed class TestTapToPayFlow : Parcelable {
        @Parcelize
        data class AfterPayment(val order: Order) : TestTapToPayFlow()

        @Parcelize
        object BeforePayment : TestTapToPayFlow()
    }
}
