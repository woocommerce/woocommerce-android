package com.woocommerce.android.ui.bookings.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class BookingListFragment : TopLevelFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun getFragmentTitle() = getString(R.string.bookings_tab_title)
    override fun shouldExpandToolbar(): Boolean = false
    override fun scrollToTop() {
        return
    }

    private val viewModel: BookingListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BookingListScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BookingListViewModel.NavigateToBookingDetails -> {
                    // TODO navigate to booking details screen
                    ToastUtils.showToast(requireContext(), "Navigate to booking ${event.bookingId}")
                }
            }
        }
    }
}
