package com.woocommerce.android.ui.bookings.details

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.bookings.BookingsCommunicationViewModel
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.payments.refunds.RefundSummaryFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class BookingDetailsFragment : BaseFragment() {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: BookingDetailsViewModel by viewModels()
    private val args: BookingDetailsFragmentArgs by navArgs()
    private val bookingsCommunicationViewModel: BookingsCommunicationViewModel by activityViewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BookingDetailsScreen(
                viewModel = viewModel,
                onBack = { findNavController().popBackStack() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
        handleOnePaneToTwoPaneConversion()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
                is BookingDetailsViewModel.NavigateToBookingNote -> {
                    findNavController().navigate(
                        BookingDetailsFragmentDirections
                            .actionBookingDetailsFragmentToBookingNoteFragment(event.bookingId)
                    )
                }
                is BookingDetailsViewModel.NavigateToRescheduleBooking -> {
                    findNavController().navigateSafely(
                        BookingDetailsFragmentDirections
                            .actionBookingDetailsFragmentToBookingRescheduleFragment(event.bookingId)
                    )
                }
                is BookingDetailsViewModel.NavigateToOrder -> {
                    requireActivity().findNavController(R.id.nav_host_fragment_main).navigate(
                        NavGraphMainDirections.actionGlobalOrderDetailFragment(
                            orderId = event.orderId,
                            ignoreTwoPaneLayoutLogic = true
                        )
                    )
                }
                is BookingDetailsViewModel.NavigateToIssueRefund -> {
                    findNavController().navigateSafely(
                        BookingDetailsFragmentDirections
                            .actionBookingDetailsFragmentToIssueRefund(
                                orderId = event.orderId
                            )
                    )
                }
            }
        }

        handleNotice(RefundSummaryFragment.REFUND_ORDER_NOTICE_KEY) {
            viewModel.onRefundCompleted()
        }
    }

    private fun handleOnePaneToTwoPaneConversion() {
        val isScreenLargerThanCompact = requireContext().isTwoPanesShouldBeUsed
        val isBookingListFragmentUpInBackStack =
            findNavController().previousBackStackEntry?.destination?.id == R.id.bookingListFragment
        if (isScreenLargerThanCompact && isBookingListFragmentUpInBackStack) {
            when (val mode = args.mode) {
                is Mode.ShowBooking -> {
                    findNavController().popBackStack()
                    bookingsCommunicationViewModel.pushEvent(
                        BookingsCommunicationViewModel.CommunicationEvent.BookingSelected(mode.bookingId)
                    )
                }

                is Mode.Empty -> {
                    findNavController().popBackStack()
                }
            }
        }
    }

    @Parcelize
    sealed class Mode : Parcelable {
        @Parcelize
        data object Empty : Mode()

        @Parcelize
        data class ShowBooking(val bookingId: Long) : Mode()
    }
}
