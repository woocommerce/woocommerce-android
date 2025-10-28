package com.woocommerce.android.ui.bookings.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BookingDetailsFragment : BaseFragment() {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: BookingDetailsViewModel by viewModels()
    private val args: BookingDetailsFragmentArgs by navArgs()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BookingDetailsScreen(
                viewModel = viewModel,
                onBack = { findNavController().popBackStack() },
                onViewOrder = { orderId ->
                    findNavController().navigate(
                        BookingDetailsFragmentDirections
                            .actionBookingDetailsFragmentToOrderDetailFragment(orderId)
                    )
                },
                onViewNotes = {
                    findNavController().navigate(
                        BookingDetailsFragmentDirections
                            .actionBookingDetailsFragmentToBookingNoteFragment(args.bookingId)
                    )
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
            }
        }
    }
}
